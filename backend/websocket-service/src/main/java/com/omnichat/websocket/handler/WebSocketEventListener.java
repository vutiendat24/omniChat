package com.omnichat.websocket.handler;

import com.omnichat.websocket.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

/**
 * Task 6.1.2.1 - WebSocket Event Listener
 *
 * Listens for STOMP session lifecycle events:
 * - SessionConnectedEvent  → Register agentId → sessionId in Redis
 * - SessionDisconnectEvent → Remove mapping from Redis
 *
 * The agentId is extracted from session attributes (set by AgentHandshakeInterceptor).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;

    /**
     * Called when a STOMP client successfully connects.
     * Registers the agentId → sessionId mapping in Redis.
     */
    @EventListener
    public void handleWebSocketConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // Get agentId from session attributes (set by handshake interceptor)
        String agentId = getAgentId(accessor);

        if (agentId != null) {
            sessionManager.registerSession(agentId, sessionId);
            log.info("Agent {} connected via WebSocket (sessionId={})", agentId, sessionId);
        } else {
            log.warn("WebSocket connected but no agentId in session attributes (sessionId={})", sessionId);
        }
    }

    /**
     * Called when a STOMP client disconnects.
     * Removes the agentId → sessionId mapping from Redis.
     */
    @EventListener
    public void handleWebSocketDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        String agentId = getAgentId(accessor);

        if (agentId != null) {
            sessionManager.removeSession(agentId);
            log.info("Agent {} disconnected from WebSocket (sessionId={})", agentId, sessionId);
        } else {
            log.debug("WebSocket disconnected with unknown agentId (sessionId={})", sessionId);
        }
    }

    /**
     * Extract agentId from STOMP session attributes.
     */
    private String getAgentId(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs != null) {
            Object agentId = sessionAttrs.get(AgentHandshakeInterceptor.AGENT_ID_ATTR);
            return agentId != null ? agentId.toString() : null;
        }
        return null;
    }
}
