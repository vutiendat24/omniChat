package com.omnichat.conversation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnichat.conversation.dto.ConversationDto;
import com.omnichat.conversation.dto.MessageDto;
import com.omnichat.conversation.dto.PaginatedResponse;
import com.omnichat.conversation.dto.SendMessageRequest;
import com.omnichat.conversation.entity.Conversation;
import com.omnichat.conversation.entity.Message;
import com.omnichat.conversation.producer.ConversationEventProducer;
import com.omnichat.conversation.repository.ConversationRepository;
import com.omnichat.conversation.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationEventProducer conversationEventProducer;

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
        // Extract fields from Facebook webhook payload structure
        JsonNode entry = eventPayload.path("entry").get(0);
        JsonNode messaging = entry.path("messaging").get(0);

        String senderId = messaging.path("sender").path("id").asText();
        String recipientId = entry.path("id").asText(); // Page ID = channelConnectionId proxy
        String messageId = messaging.path("message").path("mid").asText();
        String messageText = messaging.path("message").path("text").asText(null);

        // 1. Upsert Conversation
        Conversation conversation = conversationRepository
                .findByChannelIdentityIdAndStatus(senderId, Conversation.ConversationStatus.UNASSIGNED)
                .or(() -> conversationRepository.findByChannelIdentityIdAndStatus(senderId, Conversation.ConversationStatus.OPEN))
                .orElse(null);

        boolean isNewConversation = false;

        if (conversation == null) {
            isNewConversation = true;
            conversation = Conversation.builder()
                    .id(UUID.randomUUID().toString())
                    .channelIdentityId(senderId)
                    .channelConnectionId(Long.parseLong(recipientId.length() > 18 ? "0" : recipientId.isEmpty() ? "0" : recipientId))
                    .status(Conversation.ConversationStatus.UNASSIGNED)
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

    /**
     * Task 3.3.1.1 - GET /conversations with pagination, filtering, sorting.
     * Follows the API spec: page (1-based), limit, status filter, sort field with - prefix for DESC.
     */
    public PaginatedResponse<ConversationDto> getConversations(int page, int limit, String status, String sort) {
        // Parse sort parameter: "-last_activity_at" means DESC, "last_activity_at" means ASC
        Sort sortOrder;
        if (sort != null && !sort.isEmpty()) {
            boolean isDesc = sort.startsWith("-");
            String sortField = isDesc ? sort.substring(1) : sort;
            // Map API field names to entity field names (snake_case -> camelCase)
            String entityField = mapToEntityField(sortField);
            sortOrder = isDesc ? Sort.by(Sort.Direction.DESC, entityField) : Sort.by(Sort.Direction.ASC, entityField);
        } else {
            // Default sort: -last_activity_at (most recent first)
            sortOrder = Sort.by(Sort.Direction.DESC, "lastActivityAt");
        }

        // Spring Data uses 0-based page index, but API spec uses 1-based
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(limit, 100), sortOrder);

        Page<Conversation> conversationPage;
        if (status != null && !status.isEmpty()) {
            try {
                Conversation.ConversationStatus statusEnum = Conversation.ConversationStatus.valueOf(status.toUpperCase());
                conversationPage = conversationRepository.findByStatus(statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter: {}, returning all conversations", status);
                conversationPage = conversationRepository.findAll(pageable);
            }
        } else {
            conversationPage = conversationRepository.findAll(pageable);
        }

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
                conversation.getChannelIdentityId(), conversation.getChannelConnectionId(),
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

        // Guard: only assign if conversation is still UNASSIGNED
        if (conversation.getStatus() != Conversation.ConversationStatus.UNASSIGNED) {
            log.warn("Conversation {} is already in status {}, skipping assignment to agent {}",
                    conversationId, conversation.getStatus(), agentId);
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

    private String mapToEntityField(String apiField) {
        return switch (apiField) {
            case "last_activity_at" -> "lastActivityAt";
            case "created_at" -> "createdAt";
            case "updated_at" -> "updatedAt";
            case "status" -> "status";
            default -> "lastActivityAt";
        };
    }
}
