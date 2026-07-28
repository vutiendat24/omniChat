package com.omnichat.websocket.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.websocket.session.WebSocketSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConversationEventConsumerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private Acknowledgment acknowledgment;

    private ObjectMapper objectMapper = new ObjectMapper();

    private ConversationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new ConversationEventConsumer(messagingTemplate, sessionManager, objectMapper);
    }

    @Test
    void testConsumeConversationUpdated_WhenAgentConnected_ShouldPushToUser() throws Exception {
        // Given
        String agentId = "123";
        when(sessionManager.isAgentConnected(agentId)).thenReturn(true);

        String jsonPayload = """
                {
                    "eventType": "conversation.updated",
                    "conversationId": "conv-1",
                    "assignedAgentId": 123,
                    "status": "OPEN"
                }
                """;

        // When
        consumer.consumeConversationEvent(jsonPayload, acknowledgment);

        // Then
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(agentId),
                eq("/queue/conversations"),
                payloadCaptor.capture()
        );
        verify(acknowledgment, times(1)).acknowledge();

        Map<String, Object> sentPayload = payloadCaptor.getValue();
        assertEquals("CONVERSATION_ASSIGNED", sentPayload.get("type"));
        assertNotNull(sentPayload.get("data"));
    }

    @Test
    void testConsumeConversationUpdated_WhenAgentOffline_ShouldNotPush() throws Exception {
        // Given
        String agentId = "123";
        when(sessionManager.isAgentConnected(agentId)).thenReturn(false);

        String jsonPayload = """
                {
                    "eventType": "conversation.updated",
                    "conversationId": "conv-1",
                    "assignedAgentId": 123,
                    "status": "OPEN"
                }
                """;

        // When
        consumer.consumeConversationEvent(jsonPayload, acknowledgment);

        // Then
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void testConsumeMessageReceived_WhenAssignedAndAgentConnected_ShouldPushToSpecificUser() throws Exception {
        // Given
        String agentId = "456";
        when(sessionManager.isAgentConnected(agentId)).thenReturn(true);

        String jsonPayload = """
                {
                    "eventType": "conversation.message.received",
                    "conversationId": "conv-2",
                    "assignedAgentId": 456,
                    "conversationStatus": "OPEN"
                }
                """;

        // When
        consumer.consumeConversationEvent(jsonPayload, acknowledgment);

        // Then
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(agentId),
                eq("/queue/conversations"),
                payloadCaptor.capture()
        );
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        verify(acknowledgment, times(1)).acknowledge();

        Map<String, Object> sentPayload = payloadCaptor.getValue();
        assertEquals("NEW_MESSAGE", sentPayload.get("type"));
    }

    @Test
    void testConsumeMessageReceived_WhenUnassigned_ShouldBroadcastToTopic() throws Exception {
        // Given
        String jsonPayload = """
                {
                    "eventType": "conversation.message.received",
                    "conversationId": "conv-3",
                    "conversationStatus": "UNASSIGNED"
                }
                """;

        // When
        consumer.consumeConversationEvent(jsonPayload, acknowledgment);

        // Then
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/conversations"),
                payloadCaptor.capture()
        );
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        verify(acknowledgment, times(1)).acknowledge();

        Map<String, Object> sentPayload = payloadCaptor.getValue();
        assertEquals("NEW_MESSAGE_UNASSIGNED", sentPayload.get("type"));
    }
}
