package com.omnichat.websocket.pubsub;

import com.omnichat.websocket.session.WebSocketSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisKeyExpirationListenerTest {

    @Mock
    private RedisMessageListenerContainer listenerContainer;

    @Mock
    private WebSocketSessionManager sessionManager;

    private RedisKeyExpirationListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new RedisKeyExpirationListener(listenerContainer, sessionManager);
    }

    @Test
    void testOnMessage_WhenSessionKeyExpires_ShouldPublishOfflineEvent() {
        // Given
        String agentId = "123";
        String expiredKey = WebSocketSessionManager.SESSION_KEY_PREFIX + agentId;
        Message message = new DefaultMessage("channel".getBytes(), expiredKey.getBytes());

        // When
        listener.onMessage(message, null);

        // Then
        verify(sessionManager, times(1)).publishPresenceEvent(
                eq(agentId),
                eq(com.omnichat.websocket.event.AgentPresenceEvent.PresenceStatus.OFFLINE)
        );
    }

    @Test
    void testOnMessage_WhenOtherKeyExpires_ShouldDoNothing() {
        // Given
        String expiredKey = "some:other:key:123";
        Message message = new DefaultMessage("channel".getBytes(), expiredKey.getBytes());

        // When
        listener.onMessage(message, null);

        // Then
        verify(sessionManager, never()).publishPresenceEvent(anyString(), any());
    }
}
