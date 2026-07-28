package com.omnichat.conversation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.omnichat.conversation.dto.ConversationDto;
import com.omnichat.conversation.dto.MessageDto;
import com.omnichat.conversation.dto.PaginatedResponse;
import com.omnichat.conversation.dto.TransferRequest;
import com.omnichat.conversation.dto.UpdateStatusRequest;
import com.omnichat.conversation.dto.SendMessageRequest;
import com.omnichat.conversation.entity.Conversation;
import com.omnichat.conversation.entity.ConversationHistory;
import com.omnichat.conversation.entity.Message;
import com.omnichat.conversation.producer.ConversationEventProducer;
import com.omnichat.conversation.repository.ConversationHistoryRepository;
import com.omnichat.conversation.repository.ConversationRepository;
import com.omnichat.conversation.repository.MessageRepository;
import com.omnichat.conversation.repository.PrivateReplyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationEventProducer conversationEventProducer;
    @Mock
    private ConversationHistoryRepository conversationHistoryRepository;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private com.omnichat.conversation.repository.TagRepository tagRepository;
    @Mock
    private PrivateReplyRecordRepository privateReplyRecordRepository;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ConversationService conversationService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
    }

    @Test
    void processNormalizedIncomingMessage_WhenNoOpenConversationExists_ShouldCreateNewOpenConversation() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventType", "integration.message.received");
        payload.put("platform", "FACEBOOK");
        payload.put("externalUserId", "123");
        payload.put("messageId", "msg_1");
        payload.put("messageText", "Hello world");
        payload.put("messageType", "TEXT");
        payload.putObject("payload").put("text", "Hello world");
        payload.put("messageText", "Hello");

        when(conversationRepository.findByChannelIdentityIdAndStatus(anyString(), any()))
                .thenReturn(Optional.empty());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(messageRepository.existsById(anyString())).thenReturn(false);
        
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation c = invocation.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID().toString());
            return c;
        });

        // Act
        conversationService.processIncomingMessage(payload);

        // Assert
        ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(conversationRepository, atLeastOnce()).save(convCaptor.capture());
        verify(messageRepository, atLeastOnce()).save(messageCaptor.capture());
        
        Conversation savedConv = convCaptor.getAllValues().get(0);
        assertEquals("FACEBOOK_msg_1", messageCaptor.getValue().getId());
        assertEquals("Hello world", messageCaptor.getValue().getContentText());
        assertEquals(Message.MessageType.TEXT, messageCaptor.getValue().getMessageType());
        assertEquals("{\"text\":\"Hello world\"}", messageCaptor.getValue().getPayload());
        assertEquals(Conversation.ConversationStatus.OPEN, savedConv.getStatus());
        
        verify(conversationEventProducer).publishConversationCreated(eq(savedConv.getId()), anyString(), anyLong());
        verify(conversationEventProducer).publishConversationMessageReceived(eq(savedConv.getId()), anyString(), eq("OPEN"), isNull(), isNull(), isNull());
    }

    @Test
    void processNormalizedIncomingMessage_WhenOpenConversationExists_ShouldNotCreateNewConversation() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventType", "integration.message.received");
        payload.put("platform", "FACEBOOK");
        payload.put("externalUserId", "user-123");
        payload.put("channelConnectionId", 456L);
        payload.put("messageId", "msg-002");
        payload.put("messageText", "Hello again");

        Conversation existingConv = new Conversation();
        existingConv.setId("existing-conv-id");
        existingConv.setStatus(Conversation.ConversationStatus.OPEN);

        // Mocking the check for existing conversation
        // The implementation might check PENDING first, then OPEN. We'll just return Optional.empty() for PENDING and the conversation for OPEN
        when(conversationRepository.findByChannelIdentityIdAndStatus(eq("FACEBOOK:user-123"), eq(Conversation.ConversationStatus.PENDING)))
                .thenReturn(Optional.empty());
        when(conversationRepository.findByChannelIdentityIdAndStatus(eq("FACEBOOK:user-123"), eq(Conversation.ConversationStatus.OPEN)))
                .thenReturn(Optional.of(existingConv));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(messageRepository.existsById(anyString())).thenReturn(false);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        conversationService.processIncomingMessage(payload);

        // Assert
        verify(conversationRepository).save(existingConv);
        verify(conversationEventProducer, never()).publishConversationCreated(anyString(), anyString(), anyLong());
    }

    @Test
    void updateConversationStatus_WhenAgentChangesToResolved_ShouldUpdateStatusAndPushEvent() {
        // Arrange
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation();
        conv.setId(convId);
        conv.setStatus(Conversation.ConversationStatus.OPEN);
        conv.setAssignedAgentId(1L);

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conv);

        com.omnichat.conversation.dto.UpdateStatusRequest req = new com.omnichat.conversation.dto.UpdateStatusRequest();
        req.setStatus(Conversation.ConversationStatus.RESOLVED);
        req.setReason("Done");

        // Act
        com.omnichat.conversation.dto.ConversationDto result = conversationService.updateConversationStatus(convId, req, "1", "AGENT");

        // Then
        assertEquals("RESOLVED", result.getStatus());
        assertNotNull(conv.getClosedAt());
        verify(conversationRepository).save(conv);
        verify(conversationHistoryRepository).save(any(com.omnichat.conversation.entity.ConversationHistory.class));
        verify(conversationEventProducer).publishConversationStatusUpdated(convId, "OPEN", "RESOLVED", "1");
    }

    @Test
    void updateConversationStatus_WhenAgentIsNotAssigned_ShouldThrowAccessDenied() {
        // Arrange
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation();
        conv.setId(convId);
        conv.setStatus(Conversation.ConversationStatus.OPEN);
        conv.setAssignedAgentId(2L); // Different agent

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));

        com.omnichat.conversation.dto.UpdateStatusRequest req = new com.omnichat.conversation.dto.UpdateStatusRequest();
        req.setStatus(Conversation.ConversationStatus.RESOLVED);

        // Act & Then
        org.springframework.web.server.ResponseStatusException ex = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            conversationService.updateConversationStatus(convId, req, "1", "AGENT");
        });
        assertTrue(ex.getMessage().contains("Agent is not assigned to this conversation"));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void processNormalizedIncomingMessage_WhenDuplicateEventReceived_ShouldBeIgnored() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventType", "integration.message.received");
        payload.put("platform", "FACEBOOK");
        payload.put("externalUserId", "123");
        payload.put("messageId", "msg_1");

        when(messageRepository.existsById("FACEBOOK_msg_1")).thenReturn(true);

        // Act
        conversationService.processIncomingMessage(payload);

        // Then
        verify(conversationRepository, never()).save(any(Conversation.class));
        verify(messageRepository, never()).save(any(Message.class));
        verify(conversationEventProducer, never()).publishConversationMessageReceived(anyString(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void processIncomingMessage_WhenRecallEventReceived_ShouldUpdateMessageAsDeleted() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventType", "integration.message.recalled");
        payload.put("platform", "FACEBOOK");
        payload.put("messageId", "msg_1");

        Message msg = new Message();
        msg.setId("FACEBOOK_msg_1");
        msg.setConversationId("conv_1");
        msg.setContentText("Original Text");

        when(messageRepository.findById("FACEBOOK_msg_1")).thenReturn(Optional.of(msg));

        // Act
        conversationService.processIncomingMessage(payload);

        // Then
        verify(messageRepository).save(msg);
        assertEquals("\"Tin nhắn đã bị thu hồi\"", msg.getPayload());
        assertEquals("Tin nhắn đã bị thu hồi", msg.getContentText());
        assertTrue(msg.getIsDeleted());
        assertEquals(Message.MessageStatus.UNSENT, msg.getStatus());
        verify(conversationEventProducer).publishConversationMessageReceived("conv_1", "FACEBOOK_msg_1", "RECALLED", null, null, null);
    }

    @Test
    void getConversations_WhenAgentRequests_ShouldReturnAssignedOrUnassigned() {
        // Arrange
        Conversation conv1 = new Conversation();
        conv1.setId("conv-1");
        conv1.setStatus(Conversation.ConversationStatus.OPEN);

        Page<Conversation> mockPage = new PageImpl<>(List.of(conv1));
        when(conversationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // Act
        com.omnichat.conversation.dto.PaginatedResponse<com.omnichat.conversation.dto.ConversationDto> response = 
                conversationService.getConversations(1, 20, "OPEN", null, null, null, null, "-last_message_at", "10", "AGENT");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("conv-1", response.getData().get(0).getId());
    }

    @Test
    void getConversations_WhenAdminRequestsAnotherAgent_ShouldReturnThatAgentConversations() {
        // Arrange
        Conversation conv1 = new Conversation();
        conv1.setId("conv-1");
        conv1.setAssignedAgentId(20L);
        conv1.setStatus(Conversation.ConversationStatus.RESOLVED);

        Page<Conversation> mockPage = new PageImpl<>(List.of(conv1));
        when(conversationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // Act
        com.omnichat.conversation.dto.PaginatedResponse<com.omnichat.conversation.dto.ConversationDto> response = 
                conversationService.getConversations(1, 20, "RESOLVED", null, 20L, null, null, "-last_message_at", "10", "ADMIN");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("conv-1", response.getData().get(0).getId());
    }

    @Test
    void updateConversationTags_WhenAddTagByName_ShouldCreateAndAddTag() {
        // Arrange
        Conversation conv = new Conversation();
        conv.setId("conv-1");
        
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conv));
        
        com.omnichat.conversation.dto.ConversationTagRequest req = new com.omnichat.conversation.dto.ConversationTagRequest();
        req.setTagName("NEW_TAG");
        req.setAction("ADD");

        com.omnichat.conversation.entity.Tag newTag = com.omnichat.conversation.entity.Tag.builder()
                .id(1L).tenantId(10L).name("NEW_TAG").build();

        when(tagRepository.findByTenantIdAndName(10L, "NEW_TAG")).thenReturn(Optional.empty());
        when(tagRepository.save(any(com.omnichat.conversation.entity.Tag.class))).thenReturn(newTag);

        // Act
        conversationService.updateConversationTags("conv-1", 10L, req);

        // Then
        assertEquals(1, conv.getTags().size());
        assertTrue(conv.getTags().contains(newTag));
        verify(conversationRepository).save(conv);
    }

    @Test
    void updateConversationTags_WhenExceedLimit_ShouldThrowException() {
        // Arrange
        Conversation conv = new Conversation();
        conv.setId("conv-1");
        for (int i = 0; i < 10; i++) {
            conv.getTags().add(com.omnichat.conversation.entity.Tag.builder().id((long) i).build());
        }

        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conv));
        
        com.omnichat.conversation.dto.ConversationTagRequest req = new com.omnichat.conversation.dto.ConversationTagRequest();
        req.setTagId(99L);
        req.setAction("ADD");

        com.omnichat.conversation.entity.Tag newTag = com.omnichat.conversation.entity.Tag.builder()
                .id(99L).tenantId(10L).name("EXTRA").build();
        when(tagRepository.findById(99L)).thenReturn(Optional.of(newTag));

        // Act & Then
        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, 
                () -> conversationService.updateConversationTags("conv-1", 10L, req)
        );
        assertEquals("Cannot add more than 10 tags to a conversation", ex.getMessage());
    }

    @Test
    void sendAgentMessage_ShouldSetFirstRespondedAtAndCheckSLA() {
        // Arrange
        Conversation conv = new Conversation();
        conv.setId("conv-1");
        conv.setStatus(Conversation.ConversationStatus.OPEN);
        conv.setSlaDueAt(LocalDateTime.now().minusMinutes(5));
        conv.setIsSlABreached(false);
        conv.setFirstRespondedAt(null);

        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conv));

        SendMessageRequest req = new SendMessageRequest();
        req.setContentText("Hello");

        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        conversationService.sendAgentMessage("conv-1", req, "100");

        // Then
        assertNotNull(conv.getFirstRespondedAt());
        assertTrue(conv.getIsSlABreached());
        verify(conversationRepository).save(conv);
    }

    @Test
    void sendPrivateReply_ShouldCreatePendingConversationAndSendEvent() {
        // Arrange
        com.omnichat.conversation.dto.PrivateReplyRequest req = new com.omnichat.conversation.dto.PrivateReplyRequest();
        req.setCommentId("123");
        req.setPageId("page1");
        req.setChannelIdentityId("customer1");
        req.setMessageText("Hello");
        req.setCommentCreatedAt(LocalDateTime.now().minusDays(2));

        when(privateReplyRecordRepository.existsById("123")).thenReturn(false);
        when(conversationRepository.findByChannelIdentityIdAndStatus("customer1", Conversation.ConversationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(conversationRepository.findByChannelIdentityIdAndStatus("customer1", Conversation.ConversationStatus.OPEN))
                .thenReturn(Optional.empty());

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        MessageDto result = conversationService.sendPrivateReply(req, "agent1");

        // Then
        assertNotNull(result);
        assertEquals("Hello", result.getContentText());
        verify(privateReplyRecordRepository).save(any(com.omnichat.conversation.entity.PrivateReplyRecord.class));
        verify(conversationEventProducer).publishPrivateReplyRequested("123", "page1", result.getId(), "Hello");
    }

    @Test
    void sendPrivateReply_WhenCommentIsOlderThan7Days_ShouldThrowException() {
        // Arrange
        com.omnichat.conversation.dto.PrivateReplyRequest req = new com.omnichat.conversation.dto.PrivateReplyRequest();
        req.setCommentId("123");
        req.setCommentCreatedAt(LocalDateTime.now().minusDays(8));

        // Act & Then
        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> conversationService.sendPrivateReply(req, "agent1")
        );
        assertEquals("Đã quá thời hạn 7 ngày cho phép của Facebook", ex.getMessage());
    }

    @Test
    void sendPrivateReply_WhenAlreadyReplied_ShouldThrowException() {
        // Arrange
        com.omnichat.conversation.dto.PrivateReplyRequest req = new com.omnichat.conversation.dto.PrivateReplyRequest();
        req.setCommentId("123");
        req.setCommentCreatedAt(LocalDateTime.now().minusDays(2));

        when(privateReplyRecordRepository.existsById("123")).thenReturn(true);

        // Act & Then
        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> conversationService.sendPrivateReply(req, "agent1")
        );
        assertEquals("Bình luận này đã được gửi tin riêng (1 per comment)", ex.getMessage());
    }
}
