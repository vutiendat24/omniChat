package com.omnichat.websocket.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Task 6.1.1.1 - Handshake Interceptor
 *
 * Extracts agentId from the WebSocket handshake request and stores it
 * in the session attributes. This agentId is later used by:
 * - WebSocketEventListener: to map agentId → sessionId in Redis
 * - ConversationEventConsumer: to route messages to the correct agent's session
 *
 * The agentId can be provided via:
 * 1. Query parameter: ws://localhost:8085/ws?agentId=1
 * 2. Header: X-Agent-Id (useful when connecting through API Gateway with JWT)
 *
 * In production, agentId would be extracted from the JWT token
 * validated by the API Gateway.
 */
@Slf4j
@Component
public class AgentHandshakeInterceptor implements HandshakeInterceptor {

    public static final String AGENT_ID_ATTR = "agentId";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String agentId = null;

        // 1. Try query parameter
        if (request instanceof ServletServerHttpRequest servletRequest) {
            agentId = servletRequest.getServletRequest().getParameter("agentId");
        }

        // 2. Fallback: try header
        if (agentId == null || agentId.isBlank()) {
            var headerValues = request.getHeaders().get("X-Agent-Id");
            if (headerValues != null && !headerValues.isEmpty()) {
                agentId = headerValues.get(0);
            }
        }

        if (agentId != null && !agentId.isBlank()) {
            attributes.put(AGENT_ID_ATTR, agentId);
            log.info("WebSocket handshake: agentId={} extracted from request", agentId);
            return true;
        }

        log.warn("WebSocket handshake rejected: no agentId provided");
        return false; // Reject connection without agentId
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No-op
    }
}
