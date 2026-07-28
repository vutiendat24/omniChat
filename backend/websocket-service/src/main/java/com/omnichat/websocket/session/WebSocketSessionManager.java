package com.omnichat.websocket.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Task 6.1.2.1 - Redis Session Manager & MOD-REAL-05 Presence Sync
 *
 * Manages the mapping AgentId → WebSocket SessionId in Redis.
 *
 * Redis key design:
 * - "ws:sessions:agent:{agentId}"  → sessionId (String, TTL: 3m)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpUserRegistry simpUserRegistry;

    public static final String SESSION_KEY_PREFIX = "ws:sessions:agent:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(3);
    private static final String PRESENCE_TOPIC = "agent.presence.events";

    /**
     * Register a new WebSocket session for an agent.
     */
    public void registerSession(String agentId, String sessionId) {
        String key = SESSION_KEY_PREFIX + agentId;

        Long sizeBefore = redisTemplate.opsForSet().size(key);
        boolean isFirstSession = (sizeBefore == null || sizeBefore == 0);

        redisTemplate.opsForSet().add(key, sessionId);
        redisTemplate.expire(key, SESSION_TTL);

        log.info("Registered WS session: agentId={}, sessionId={}", agentId, sessionId);

        if (isFirstSession) {
            publishPresenceEvent(agentId, com.omnichat.websocket.event.AgentPresenceEvent.PresenceStatus.ONLINE);
        }
    }

    /**
     * Remove an agent's WebSocket session.
     */
    public void removeSession(String agentId, String sessionId) {
        String key = SESSION_KEY_PREFIX + agentId;

        redisTemplate.opsForSet().remove(key, sessionId);
        Long size = redisTemplate.opsForSet().size(key);
        
        if (size == null || size == 0) {
            log.info("Removed last WS session for agent: agentId={}", agentId);
            publishPresenceEvent(agentId, com.omnichat.websocket.event.AgentPresenceEvent.PresenceStatus.OFFLINE);
            // Optionally delete the key explicitly if size is 0
            redisTemplate.delete(key);
        } else {
            log.info("Removed WS session: agentId={}, sessionId={}. Remaining sessions: {}", agentId, sessionId, size);
        }
    }

    /**
     * Get the session IDs for a connected agent.
     */
    public Set<String> getSessionIds(String agentId) {
        String key = SESSION_KEY_PREFIX + agentId;
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * Check if an agent currently has an active WebSocket connection.
     */
    public boolean isAgentConnected(String agentId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SESSION_KEY_PREFIX + agentId));
    }

    /**
     * Heartbeat: Refresh TTL for all users connected to THIS node.
     * Runs every 1 minute.
     */
    @Scheduled(fixedRate = 60000)
    public void refreshSessionTtls() {
        Set<SimpUser> users = simpUserRegistry.getUsers();
        if (users.isEmpty()) return;

        int count = 0;
        for (SimpUser user : users) {
            String agentId = user.getName();
            String key = SESSION_KEY_PREFIX + agentId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.expire(key, SESSION_TTL);
                count++;
            }
        }
        log.debug("Refreshed Redis TTL for {} active local agents", count);
    }

    public void publishPresenceEvent(String agentId, com.omnichat.websocket.event.AgentPresenceEvent.PresenceStatus status) {
        com.omnichat.websocket.event.AgentPresenceEvent event = com.omnichat.websocket.event.AgentPresenceEvent.builder()
                .agentId(agentId)
                .status(status)
                .timestamp(System.currentTimeMillis())
                .build();
        kafkaTemplate.send(PRESENCE_TOPIC, agentId, event);
        log.info("Published {} event for agentId={}", status, agentId);
    }
}
