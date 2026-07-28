package com.omnichat.websocket.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WebSocketSessionManagerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private WebSocketSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void testRegisterSession_ShouldAddSessionToSet() {
        String agentId = "agent-1";
        String sessionId = "session-1";

        sessionManager.registerSession(agentId, sessionId);

        verify(setOperations, times(1)).add("ws:sessions:agent:" + agentId, sessionId);
        verify(setOperations, times(1)).add("ws:sessions:active", agentId);
    }

    @Test
    void testRemoveSession_WhenMultipleSessionsExist_ShouldRemoveOneSessionAndKeepAgentActive() {
        String agentId = "agent-1";
        String sessionId = "session-1";

        when(setOperations.size("ws:sessions:agent:" + agentId)).thenReturn(1L); // Before removing, there are 2 sessions (size returns 1 after remove? wait, we mock size to return > 0)

        sessionManager.removeSession(agentId, sessionId);

        verify(setOperations, times(1)).remove("ws:sessions:agent:" + agentId, sessionId);
        // Should not remove from active if size > 0
        verify(setOperations, never()).remove("ws:sessions:active", agentId);
    }

    @Test
    void testRemoveSession_WhenLastSession_ShouldRemoveFromActive() {
        String agentId = "agent-1";
        String sessionId = "session-1";

        when(setOperations.size("ws:sessions:agent:" + agentId)).thenReturn(0L);

        sessionManager.removeSession(agentId, sessionId);

        verify(setOperations, times(1)).remove("ws:sessions:agent:" + agentId, sessionId);
        verify(setOperations, times(1)).remove("ws:sessions:active", agentId);
    }
}
