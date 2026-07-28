package com.omnichat.websocket.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Task 6.1.2.1 - Redis Session Manager
 *
 * Manages the mapping AgentId → WebSocket SessionId in Redis.
 * Per Architecture_Design_OCM.md §7 Caching Strategy:
 *   "Redis Pub/Sub kết hợp với bộ nhớ Redis được dùng để lưu trữ ánh xạ:
 *    AgentId → WebSocket Session Server Instance"
 *
 * Redis key design:
 * - "ws:session:agent:{agentId}"  → sessionId (String, TTL: 24h)
 * - "ws:sessions:active"          → Set of agentId strings with active WS connections
 *
 * This allows the Kafka consumer to quickly determine:
 * 1. Whether an agent is currently connected via WebSocket
 * 2. Which sessionId to target for per-agent messaging
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    private static final String SESSION_KEY_PREFIX = "ws:sessions:agent:";
    private static final String ACTIVE_SESSIONS_KEY = "ws:sessions:active";
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final String PRESENCE_TOPIC = "agent.presence.events";

    /**
     * Register a new WebSocket session for an agent.
     * Called when a STOMP CONNECT succeeds (from WebSocketEventListener).
     *
     * @param agentId   the agent's ID
     * @param sessionId the WebSocket session ID
     */
    public void registerSession(String agentId, String sessionId) {
        String key = SESSION_KEY_PREFIX + agentId;

        Long sizeBefore = redisTemplate.opsForSet().size(key);
        boolean isFirstSession = (sizeBefore == null || sizeBefore == 0);

        redisTemplate.opsForSet().add(key, sessionId);
        redisTemplate.expire(key, SESSION_TTL);
        redisTemplate.opsForSet().add(ACTIVE_SESSIONS_KEY, agentId);

        log.info("Registered WS session: agentId={}, sessionId={}", agentId, sessionId);

        if (isFirstSession) {
            com.omnichat.websocket.event.AgentPresenceEvent event = com.omnichat.websocket.event.AgentPresenceEvent.builder()
                    .agentId(agentId)
                    .status(com.omnichat.websocket.event.AgentPresenceEvent.PresenceStatus.ONLINE)
                    .timestamp(System.currentTimeMillis())
                    .build();
            kafkaTemplate.send(PRESENCE_TOPIC, agentId, event);
            log.info("Published ONLINE event for agentId={}", agentId);
        }
    }

    /**
     * Remove an agent's WebSocket session.
     * Called when STOMP DISCONNECT or session timeout (from WebSocketEventListener).
     *
     * @param agentId the agent whose session to remove
     * @param sessionId the session to remove
     */
    public void removeSession(String agentId, String sessionId) {
        String key = SESSION_KEY_PREFIX + agentId;

        redisTemplate.opsForSet().remove(key, sessionId);
        Long size = redisTemplate.opsForSet().size(key);
        
        if (size == null || size == 0) {
            redisTemplate.opsForSet().remove(ACTIVE_SESSIONS_KEY, agentId);
            log.info("Removed last WS session for agent: agentId={}", agentId);

            com.omnichat.websocket.event.AgentPresenceEvent event = com.omnichat.websocket.event.AgentPresenceEvent.builder()
                    .agentId(agentId)
                    .status(com.omnichat.websocket.event.AgentPresenceEvent.PresenceStatus.OFFLINE)
                    .timestamp(System.currentTimeMillis())
                    .build();
            kafkaTemplate.send(PRESENCE_TOPIC, agentId, event);
            log.info("Published OFFLINE event for agentId={}", agentId);
        } else {
            log.info("Removed WS session: agentId={}, sessionId={}. Remaining sessions: {}", agentId, sessionId, size);
        }
    }

    /**
     * Get the session IDs for a connected agent.
     *
     * @param agentId the agent's ID
     * @return set of sessionIds, or empty if agent is not connected
     */
    public Set<String> getSessionIds(String agentId) {
        String key = SESSION_KEY_PREFIX + agentId;
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * Check if an agent currently has an active WebSocket connection.
     *
     * @param agentId the agent to check
     * @return true if agent has an active session
     */
    public boolean isAgentConnected(String agentId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(ACTIVE_SESSIONS_KEY, agentId));
    }

    /**
     * Get all currently connected agent IDs.
     *
     * @return Set of agentId strings
     */
    public Set<String> getConnectedAgentIds() {
        Set<String> members = redisTemplate.opsForSet().members(ACTIVE_SESSIONS_KEY);
        return members != null ? members : Set.of();
    }
}
