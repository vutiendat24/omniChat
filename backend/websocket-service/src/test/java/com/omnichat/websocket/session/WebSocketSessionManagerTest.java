package com.omnichat.websocket.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.simp.user.SimpUserRegistry;

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

    @Mock
    private SimpUserRegistry simpUserRegistry;

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
        // Verify TTL is set
        verify(redisTemplate, times(1)).expire(eq("ws:sessions:agent:" + agentId), any());
    }

    @Test
    void testRemoveSession_WhenMultipleSessionsExist_ShouldNotPublishOfflineEvent() {
        String agentId = "agent-1";
        String sessionId = "session-1";

        when(setOperations.size("ws:sessions:agent:" + agentId)).thenReturn(1L);

        sessionManager.removeSession(agentId, sessionId);

        verify(setOperations, times(1)).remove("ws:sessions:agent:" + agentId, sessionId);
        verify(kafkaTemplate, never()).send(eq("agent.presence.events"), eq(agentId), any());
    }

    @Test
    void testRemoveSession_WhenLastSession_ShouldPublishOfflineEvent() {
        String agentId = "agent-1";
        String sessionId = "session-1";

        when(setOperations.size("ws:sessions:agent:" + agentId)).thenReturn(0L);

        sessionManager.removeSession(agentId, sessionId);

        verify(setOperations, times(1)).remove("ws:sessions:agent:" + agentId, sessionId);
        verify(kafkaTemplate, times(1)).send(eq("agent.presence.events"), eq(agentId), any());
    }

    @Test
    void testRefreshSessionTtls_ShouldUpdateTtlForConnectedUsers() {
        org.springframework.messaging.simp.user.SimpUser mockUser = mock(org.springframework.messaging.simp.user.SimpUser.class);
        when(mockUser.getName()).thenReturn("agent-1");
        when(simpUserRegistry.getUsers()).thenReturn(java.util.Set.of(mockUser));
        when(redisTemplate.hasKey("ws:sessions:agent:agent-1")).thenReturn(true);

        sessionManager.refreshSessionTtls();

        verify(redisTemplate, times(1)).expire(eq("ws:sessions:agent:agent-1"), eq(java.time.Duration.ofMinutes(3)));
    }
}

