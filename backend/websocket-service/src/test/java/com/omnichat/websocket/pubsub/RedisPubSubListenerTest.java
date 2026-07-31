package com.omnichat.websocket.pubsub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisPubSubListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SimpUserRegistry simpUserRegistry;

    @InjectMocks
    private RedisPubSubListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHandleMessage_WhenMessageFromSelf_ShouldIgnore() {
        // Given
        SyncMessage message = new SyncMessage();
        message.setSourceNodeId(RedisPubSubListener.NODE_ID); // Same node ID
        message.setTargetAgentId("123");

        // When
        listener.handleMessage(message);

        // Then
        verify(simpUserRegistry, never()).getUser(anyString());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void testHandleMessage_WhenUserConnectedLocally_ShouldPushToUser() {
        // Given
        SyncMessage message = new SyncMessage();
        message.setSourceNodeId("OTHER_NODE_ID");
        message.setTargetAgentId("123");
        message.setDestination("/queue/conversations");
        Map<String, Object> payload = new HashMap<>();
        payload.put("test", "data");
        message.setPayload(payload);

        SimpUser mockUser = mock(SimpUser.class);
        when(mockUser.hasSessions()).thenReturn(true);
        when(simpUserRegistry.getUser("123")).thenReturn(mockUser);

        // When
        listener.handleMessage(message);

        // Then
        verify(messagingTemplate, times(1)).convertAndSendToUser("123", "/queue/conversations", payload);
    }

    @Test
    void testHandleMessage_WhenUserNotConnectedLocally_ShouldNotPush() {
        // Given
        SyncMessage message = new SyncMessage();
        message.setSourceNodeId("OTHER_NODE_ID");
        message.setTargetAgentId("123");
        
        when(simpUserRegistry.getUser("123")).thenReturn(null);

        // When
        listener.handleMessage(message);

        // Then
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }
}
