package com.omnichat.conversation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnichat.conversation.dto.ConversationDto;
import com.omnichat.conversation.dto.MessageDto;
import com.omnichat.conversation.dto.PaginatedResponse;
import com.omnichat.conversation.dto.SendMessageRequest;
import com.omnichat.conversation.dto.TransferRequest;
import com.omnichat.conversation.dto.UpdateStatusRequest;
import com.omnichat.conversation.entity.Conversation;
import com.omnichat.conversation.entity.ConversationHistory;
import com.omnichat.conversation.entity.Message;
import com.omnichat.conversation.producer.ConversationEventProducer;
import com.omnichat.conversation.repository.ConversationRepository;
import com.omnichat.conversation.repository.ConversationHistoryRepository;
import com.omnichat.conversation.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationHistoryRepository conversationHistoryRepository;
    private final ConversationEventProducer conversationEventProducer;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Task 3.2.2.1 - Upsert Conversation and Insert Message.
     * Called by IntegrationEventConsumer when a new webhook message arrives.
     *
     * Logic:
     * 1. Extract senderId from the integration event payload (used as channelIdentityId).
     * 2. Find an existing OPEN or UNASSIGNED conversation for this channelIdentityId.
     *    - If found: reuse it (update lastActivityAt).
     *    - If not found: create a new conversation with status UNASSIGNED.
     * 3. Insert the message into the messages table.
     * 4. Publish a ConversationMessageReceived event so routing-service can assign an agent.
     */
    @Transactional
    public void processIncomingMessage(JsonNode eventPayload) {
        if ("integration.message.received".equals(eventPayload.path("eventType").asText(""))) {
            processNormalizedIncomingMessage(eventPayload);
            return;
        }
        if ("integration.message.recalled".equals(eventPayload.path("eventType").asText(""))) {
            processMessageRecall(eventPayload);
            return;
        }

        // Extract fields from Facebook webhook payload structure
        JsonNode entry = eventPayload.path("entry").get(0);
        JsonNode messaging = entry.path("messaging").get(0);

        String senderId = messaging.path("sender").path("id").asText();
        String recipientId = entry.path("id").asText(); // Page ID = channelConnectionId proxy
        String messageId = messaging.path("message").path("mid").asText();
        String messageText = messaging.path("message").path("text").asText(null);

        // 1. Upsert Conversation
        Conversation conversation = conversationRepository
                .findByChannelIdentityIdAndStatus(senderId, Conversation.ConversationStatus.PENDING)
                .or(() -> conversationRepository.findByChannelIdentityIdAndStatus(senderId, Conversation.ConversationStatus.OPEN))
                .orElse(null);

        boolean isNewConversation = false;

        if (conversation == null) {
            isNewConversation = true;
            conversation = Conversation.builder()
                    .id(UUID.randomUUID().toString())
                    .channelIdentityId(senderId)
                    .channelConnectionId(Long.parseLong(recipientId.length() > 18 ? "0" : recipientId.isEmpty() ? "0" : recipientId))
                    .status(Conversation.ConversationStatus.OPEN)
                    .lastActivityAt(LocalDateTime.now())
                    .build();
            conversation = conversationRepository.save(conversation);
            log.info("Created new conversation: {}", conversation.getId());
        } else {
            conversation.setLastActivityAt(LocalDateTime.now());
            conversation = conversationRepository.save(conversation);
            log.info("Updated existing conversation: {}", conversation.getId());
        }

        // 2. Insert Message
        Message message = Message.builder()
                .id(messageId != null && !messageId.isEmpty() ? messageId : UUID.randomUUID().toString())
                .conversationId(conversation.getId())
                .senderType(Message.SenderType.CUSTOMER)
                .senderId(senderId)
                .contentText(messageText)
                .status(Message.MessageStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();
        messageRepository.save(message);
        log.info("Saved message: {} for conversation: {}", message.getId(), conversation.getId());

        // 3. Task 3.2.3.1 - Publish ConversationMessageReceived event (for routing if UNASSIGNED)
        if (isNewConversation) {
            conversationEventProducer.publishConversationCreated(
                    conversation.getId(), senderId, conversation.getChannelConnectionId());
        }
        conversationEventProducer.publishConversationMessageReceived(
                conversation.getId(), message.getId(), conversation.getStatus().name(),
                null, null, null); // Customer-originated: no outbound push needed
    }

    private void processNormalizedIncomingMessage(JsonNode eventPayload) {
        String platform = eventPayload.path("platform").asText("UNKNOWN").toUpperCase();
        String externalUserId = eventPayload.path("externalUserId").asText();
        Long channelConnectionId = eventPayload.path("channelConnectionId").asLong(0L);
        String rawMessageId = eventPayload.path("messageId").asText(UUID.randomUUID().toString());
        String messageId = normalizeExternalMessageId(platform, rawMessageId);
        String messageText = eventPayload.path("messageText").asText(null);
        String payload = eventPayload.path("payload").isMissingNode() ? null : eventPayload.path("payload").toString();
        String messageTypeStr = eventPayload.path("messageType").asText("TEXT").toUpperCase();
        Message.MessageType messageType = Message.MessageType.valueOf(messageTypeStr);
        Long originCreatedAt = eventPayload.path("originCreatedAt").asLong(0L);
        String channelIdentityId = platform + ":" + externalUserId;

        if (externalUserId == null || externalUserId.isBlank()) {
            throw new IllegalArgumentException("externalUserId is required");
        }

        // Idempotency Check
        if (messageRepository.existsById(messageId)) {
            log.info("Message {} already processed, skipping duplicate event", messageId);
            return;
        }

        // Distributed Lock to prevent Race Condition
        String lockKey = "lock:conversation:" + channelIdentityId;
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 10, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(lockAcquired)) {
            log.warn("Could not acquire lock for {}, another thread is creating conversation. Throwing to retry.", channelIdentityId);
            throw new RuntimeException("Could not acquire lock for conversation creation");
        }

        try {
            Conversation conversation = findActiveConversation(channelIdentityId);
            if (conversation == null && "FACEBOOK".equals(platform)) {
                conversation = findActiveConversation(externalUserId);
            }

            boolean isNewConversation = false;

            LocalDateTime now = LocalDateTime.now();
            String preview = messageText != null ? (messageText.length() > 50 ? messageText.substring(0, 50) + "..." : messageText) : "[" + messageTypeStr + "]";

            if (conversation == null) {
                isNewConversation = true;
                conversation = Conversation.builder()
                        .id(UUID.randomUUID().toString())
                        .channelIdentityId(channelIdentityId)
                        .channelConnectionId(channelConnectionId)
                        .status(Conversation.ConversationStatus.OPEN)
                        .lastActivityAt(now)
                        .lastMessageAt(now)
                        .lastMessagePreview(preview)
                        .build();
                conversation = conversationRepository.save(conversation);
                log.info("Created new {} conversation: {}", platform, conversation.getId());
            } else {
                conversation.setLastActivityAt(now);
                conversation.setLastMessageAt(now);
                conversation.setLastMessagePreview(preview);
                conversation = conversationRepository.save(conversation);
                log.info("Updated existing {} conversation: {}", platform, conversation.getId());
            }

            Message message = Message.builder()
                    .id(messageId)
                    .conversationId(conversation.getId())
                    .senderType(Message.SenderType.CUSTOMER)
                    .senderId(channelIdentityId)
                    .messageType(messageType)
                    .payload(payload)
                    .contentText(messageText)
                    .status(Message.MessageStatus.SENT)
                    .sentAt(now)
                    .originCreatedAt(originCreatedAt > 0 ? java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(originCreatedAt), java.time.ZoneId.systemDefault()) : null)
                    .build();
            messageRepository.save(message);
            log.info("Saved {} message: {} for conversation: {}", platform, message.getId(), conversation.getId());

            if (isNewConversation) {
                conversationEventProducer.publishConversationCreated(
                        conversation.getId(), channelIdentityId, conversation.getChannelConnectionId());
            }
            conversationEventProducer.publishConversationMessageReceived(
                    conversation.getId(), message.getId(), conversation.getStatus().name(),
                    null, null, null);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void processMessageRecall(JsonNode eventPayload) {
        String platform = eventPayload.path("platform").asText("UNKNOWN").toUpperCase();
        String rawMessageId = eventPayload.path("messageId").asText("");
        String messageId = normalizeExternalMessageId(platform, rawMessageId);

        if (messageId.isBlank()) {
            return;
        }

        Message message = messageRepository.findById(messageId).orElse(null);
        if (message != null) {
            message.setPayload("\"Tin nhắn đã bị thu hồi\"");
            message.setContentText("Tin nhắn đã bị thu hồi"); // Update contentText for backward compatibility
            message.setIsDeleted(true);
            message.setStatus(Message.MessageStatus.UNSENT);
            messageRepository.save(message);
            log.info("Message {} was recalled and marked as deleted", messageId);
            
            conversationEventProducer.publishConversationMessageReceived(
                    message.getConversationId(), message.getId(), "RECALLED",
                    null, null, null); // Signal UI
        } else {
            log.warn("Received recall event for unknown message {}", messageId);
        }
    }

    /**
     * Task 3.3.1.1 - GET /conversations with pagination, filtering, sorting.
     * Follows the API spec: page (1-based), limit, status filter, sort field with - prefix for DESC.
     */
    public PaginatedResponse<ConversationDto> getConversations(
            int page, int limit, String status, Long channelId, Long agentId, Long tagId,
            String searchKeyword, String sort, String currentUserId, String currentUserRole) {
        
        // Parse sort parameter: "-last_message_at" means DESC, "last_message_at" means ASC
        Sort sortOrder;
        if (sort != null && !sort.isEmpty()) {
            boolean isDesc = sort.startsWith("-");
            String sortField = isDesc ? sort.substring(1) : sort;
            String entityField = mapToEntityField(sortField);
            sortOrder = isDesc ? Sort.by(Sort.Direction.DESC, entityField) : Sort.by(Sort.Direction.ASC, entityField);
        } else {
            sortOrder = Sort.by(Sort.Direction.DESC, "lastMessageAt");
        }

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(limit, 100), sortOrder);

        Specification<Conversation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUserRole) || "SUPERVISOR".equalsIgnoreCase(currentUserRole);
            if (!isAdmin) {
                Long currentAgentId = currentUserId != null ? Long.parseLong(currentUserId) : null;
                Long finalAgentId = agentId;
                if (agentId != null && !agentId.equals(currentAgentId)) {
                    finalAgentId = currentAgentId; // Ignore requested agentId and force to current agent
                }
                
                if (finalAgentId != null) {
                    predicates.add(cb.equal(root.get("assignedAgentId"), finalAgentId));
                } else {
                    predicates.add(cb.or(
                        cb.equal(root.get("assignedAgentId"), currentAgentId),
                        cb.isNull(root.get("assignedAgentId"))
                    ));
                }
            } else {
                if (agentId != null) {
                    predicates.add(cb.equal(root.get("assignedAgentId"), agentId));
                }
            }

            if (status != null && !status.isEmpty()) {
                try {
                    Conversation.ConversationStatus statusEnum = Conversation.ConversationStatus.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid status
                }
            } else if (!isAdmin && (searchKeyword == null || searchKeyword.isBlank())) {
                predicates.add(cb.equal(root.get("status"), Conversation.ConversationStatus.OPEN));
            }

            if (channelId != null) {
                predicates.add(cb.equal(root.get("channelConnectionId"), channelId));
            }
            
            if (searchKeyword != null && !searchKeyword.isBlank()) {
                String likePattern = "%" + searchKeyword.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("customerName")), likePattern),
                    cb.like(root.get("customerPhone"), likePattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Conversation> conversationPage = conversationRepository.findAll(spec, pageable);

        return PaginatedResponse.<ConversationDto>builder()
                .data(conversationPage.getContent().stream()
                        .map(ConversationDto::fromEntity)
                        .toList())
                .meta(PaginatedResponse.Meta.builder()
                        .currentPage(page)
                        .totalPages(conversationPage.getTotalPages())
                        .totalItems(conversationPage.getTotalElements())
                        .build())
                .build();
    }

    /**
     * Task 3.3.1.2 - GET /conversations/{id}/messages
     * Returns paginated message history for a conversation, sorted by sent_at DESC (newest first).
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<MessageDto> getMessages(String conversationId, int page, int limit) {
        // Verify conversation exists
        if (!conversationRepository.existsById(conversationId)) {
            throw new jakarta.persistence.EntityNotFoundException("Conversation not found: " + conversationId);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(limit, 100));

        Page<Message> messagePage = messageRepository.findByConversationIdOrderBySentAtDesc(conversationId, pageable);

        return PaginatedResponse.<MessageDto>builder()
                .data(messagePage.getContent().stream()
                        .map(MessageDto::fromEntity)
                        .toList())
                .meta(PaginatedResponse.Meta.builder()
                        .currentPage(page)
                        .totalPages(messagePage.getTotalPages())
                        .totalItems(messagePage.getTotalElements())
                        .build())
                .build();
    }

    /**
     * Task 3.3.2.1 - POST /conversations/{id}/messages
     * Agent sends a message into a conversation.
     * Validation: at least content_text or content_attachments must be present.
     */
    @Transactional
    public MessageDto sendAgentMessage(String conversationId, SendMessageRequest request, String agentId) {
        // 1. Validate request body (anyOf: content_text or content_attachments)
        if (!request.isValid()) {
            throw new IllegalArgumentException("At least one of content_text or content_attachments is required");
        }

        // 2. Verify conversation exists
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Conversation not found: " + conversationId));

        // 3. Serialize attachments to JSON string
        String attachmentsJson = null;
        if (request.getContentAttachments() != null && !request.getContentAttachments().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                attachmentsJson = mapper.writeValueAsString(request.getContentAttachments());
            } catch (Exception e) {
                log.error("Failed to serialize attachments", e);
            }
        }

        // 4. Create and save message
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .senderType(Message.SenderType.AGENT)
                .senderId(agentId)
                .contentText(request.getContentText())
                .contentAttachments(attachmentsJson)
                .status(Message.MessageStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();
        messageRepository.save(message);
        log.info("Agent {} sent message {} in conversation {}", agentId, message.getId(), conversationId);

        // 5. Update conversation last_activity_at
        conversation.setLastActivityAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // 6. Publish event to Kafka for integration-service to deliver to external channel
        conversationEventProducer.publishConversationMessageReceived(
                conversationId, message.getId(), conversation.getStatus().name(),
                extractExternalUserId(conversation.getChannelIdentityId()), conversation.getChannelConnectionId(),
                request.getContentText());

        return MessageDto.fromEntity(message);
    }

    /**
     * Task 3.4.1.1 - Handle RouteAssigned event from Routing Service.
     *
     * Called by RouteAssignedConsumer when the routing algorithm has selected an agent.
     *
     * Flow:
     * 1. Find the conversation by ID (throw 404 if not found)
     * 2. Update status: UNASSIGNED → OPEN
     * 3. Set assignedAgentId to the selected agent
     * 4. Update lastActivityAt
     * 5. Persist to MySQL
     * 6. Publish ConversationUpdated event (for WebSocket Service to push to Agent UI)
     *
     * @param conversationId the conversation to assign
     * @param agentId the agent selected by the routing algorithm
     */
    @Transactional
    public void handleRouteAssigned(String conversationId, Long agentId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Conversation not found: " + conversationId));

        // Guard: only assign if conversation is not assigned
        if (conversation.getAssignedAgentId() != null) {
            log.warn("Conversation {} is already assigned to {}, skipping assignment to agent {}",
                    conversationId, conversation.getAssignedAgentId(), agentId);
            return;
        }

        // Update conversation: UNASSIGNED → OPEN, assign agent
        Conversation.ConversationStatus oldStatus = conversation.getStatus();
        conversation.setStatus(Conversation.ConversationStatus.OPEN);
        conversation.setAssignedAgentId(agentId);
        conversation.setLastActivityAt(LocalDateTime.now());

        conversationRepository.save(conversation);
        log.info("Conversation {} assigned to agent {}: {} → OPEN",
                conversationId, agentId, oldStatus);

        // Publish ConversationUpdated event for WebSocket Service (real-time push to Agent UI)
        conversationEventProducer.publishConversationUpdated(
                conversationId, agentId, "OPEN");
    }

    /**
     * UC-303 - Manual conversation transfer.
     *
     * API: PATCH /api/v1/conversations/{id}/assign
     *
     * Per PRD §3.3 UC-303:
     *   Agent/Supervisor selects a target Agent from online members list
     *   → optionally enters a transfer reason
     *   → system transfers ownership to the new Agent.
     *   The old Agent loses messaging rights (except Supervisor/Admin).
     *
     * Per RBAC matrix (PRD §2.2):
     *   Admin: R/W | Supervisor: R/W | Agent: R (Chỉ chuyển - can only transfer their own)
     *
     * Flow:
     * 1. Validate request (targetAgentId required, > 0)
     * 2. Find conversation (404 if not found)
     * 3. Guard: conversation must be OPEN or UNASSIGNED (cannot transfer CLOSED)
     * 4. Guard: cannot transfer to the same agent
     * 5. Update assignedAgentId to the target agent
     * 6. If conversation was UNASSIGNED, change status to OPEN
     * 7. Insert a SYSTEM message recording the transfer
     * 8. Publish conversation.transferred event to Kafka:
     *    - Routing Service → decrement old agent workload, increment new agent workload
     *    - WebSocket Service → notify both old and new agents in real-time
     *
     * @param conversationId the conversation to transfer
     * @param request        contains targetAgentId and optional reason
     * @param requestingAgentId the agent performing the transfer (from JWT/header)
     * @return updated ConversationDto
     */
    @Transactional
    public ConversationDto transferConversation(String conversationId, TransferRequest request, String requestingAgentId) {
        // 1. Validate request
        if (!request.isValid()) {
            throw new IllegalArgumentException("targetAgentId is required and must be a positive number");
        }

        // 2. Find conversation
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Conversation not found: " + conversationId));

        // 3. Guard: only OPEN or PENDING conversations can be transferred
        if (conversation.getStatus() == Conversation.ConversationStatus.RESOLVED) {
            throw new IllegalStateException("Cannot transfer a RESOLVED conversation: " + conversationId);
        }

        // 4. Guard: cannot transfer to the same agent
        Long oldAgentId = conversation.getAssignedAgentId();
        Long newAgentId = request.getTargetAgentId();

        if (oldAgentId != null && oldAgentId.equals(newAgentId)) {
            throw new IllegalArgumentException(
                    "Cannot transfer conversation to the same agent: " + newAgentId);
        }

        // 5. Update conversation: assign to new agent
        conversation.setAssignedAgentId(newAgentId);

        // If unassigned → OPEN (manual assignment by Supervisor/Admin)
        if (conversation.getAssignedAgentId() == null) {
            conversation.setStatus(Conversation.ConversationStatus.OPEN);
        }

        conversation.setLastActivityAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        log.info("Conversation {} transferred: agent {} → agent {} (by agent {}, reason: {})",
                conversationId, oldAgentId, newAgentId, requestingAgentId,
                request.getReason() != null ? request.getReason() : "N/A");

        // 6. Insert a SYSTEM message recording the transfer (audit trail)
        String transferNote = String.format("Conversation transferred from Agent %s to Agent %s",
                oldAgentId != null ? oldAgentId : "unassigned", newAgentId);
        if (request.getReason() != null && !request.getReason().isBlank()) {
            transferNote += ". Reason: " + request.getReason();
        }

        Message systemMessage = Message.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .senderType(Message.SenderType.SYSTEM)
                .senderId("system")
                .contentText(transferNote)
                .status(Message.MessageStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();
        messageRepository.save(systemMessage);

        // 7. Publish conversation.transferred event to Kafka
        conversationEventProducer.publishConversationTransferred(
                conversationId,
                oldAgentId != null ? oldAgentId : 0L,
                newAgentId,
                request.getReason());

        return ConversationDto.fromEntity(conversation);
    }

    /**
     * MOD-CONV-02: Cập nhật trạng thái hội thoại.
     */
    @Transactional
    public ConversationDto updateConversationStatus(String conversationId, UpdateStatusRequest request, String agentId, String role) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Conversation not found: " + conversationId));

        // 2. Kiểm tra quyền
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role) || "SUPERVISOR".equalsIgnoreCase(role);
        if (!isAdmin) {
            // Agent chỉ được đổi trạng thái hội thoại mà họ được phân công
            if (conversation.getAssignedAgentId() == null || !conversation.getAssignedAgentId().toString().equals(agentId)) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Agent is not assigned to this conversation");
            }
        }

        Conversation.ConversationStatus oldStatus = conversation.getStatus();
        Conversation.ConversationStatus newStatus = request.getStatus();

        if (oldStatus == newStatus) {
            return ConversationDto.fromEntity(conversation);
        }

        // 3. Kiểm tra luồng trạng thái hợp lệ
        if (oldStatus == Conversation.ConversationStatus.SPAM && newStatus == Conversation.ConversationStatus.OPEN && !isAdmin) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Only Admin can change status from SPAM to OPEN");
        }

        // 4. Thực thi cập nhật
        conversation.setStatus(newStatus);
        if (newStatus == Conversation.ConversationStatus.RESOLVED) {
            conversation.setClosedAt(LocalDateTime.now());
            // Free agent capacity could be done via event to routing-service
        }

        // Save conversation - Optimistic Locking will throw exception if modified concurrently
        conversation = conversationRepository.save(conversation);

        // 5. Ghi nhận Audit Log
        ConversationHistory history = ConversationHistory.builder()
                .conversationId(conversationId)
                .changedBy(agentId != null && !agentId.isBlank() ? agentId : "SYSTEM")
                .oldStatus(oldStatus.name())
                .newStatus(newStatus.name())
                .reason(request.getReason())
                .build();
        conversationHistoryRepository.save(history);

        // 6. Đẩy sự kiện
        conversationEventProducer.publishConversationStatusUpdated(conversationId, oldStatus.name(), newStatus.name(), agentId);

        return ConversationDto.fromEntity(conversation);
    }

    private String mapToEntityField(String apiField) {
        return switch (apiField) {
            case "last_activity_at" -> "lastActivityAt";
            case "created_at" -> "createdAt";
            case "updated_at" -> "updatedAt";
            case "status" -> "status";
            default -> "lastActivityAt";
        };
    }

    private String normalizeExternalMessageId(String platform, String messageId) {
        String normalizedPlatform = platform != null && !platform.isBlank() ? platform.toLowerCase() : "external";
        String normalizedMessageId = messageId != null && !messageId.isBlank() ? messageId : UUID.randomUUID().toString();
        if (normalizedMessageId.startsWith(normalizedPlatform + ":")) {
            return normalizedMessageId;
        }
        return normalizedPlatform + ":" + normalizedMessageId;
    }

    private String extractExternalUserId(String channelIdentityId) {
        if (channelIdentityId == null) {
            return "";
        }
        int separatorIndex = channelIdentityId.indexOf(':');
        if (separatorIndex < 0 || separatorIndex == channelIdentityId.length() - 1) {
            return channelIdentityId;
        }
        return channelIdentityId.substring(separatorIndex + 1);
    }

    private Conversation findActiveConversation(String channelIdentityId) {
        return conversationRepository
                .findByChannelIdentityIdAndStatus(channelIdentityId, Conversation.ConversationStatus.PENDING)
                .or(() -> conversationRepository.findByChannelIdentityIdAndStatus(
                        channelIdentityId, Conversation.ConversationStatus.OPEN))
                .orElse(null);
    }
}
