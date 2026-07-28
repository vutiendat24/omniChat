package com.omnichat.websocket.handler;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AgentHandshakeInterceptor implements HandshakeInterceptor {

    public static final String AGENT_ID_ATTR = "agentId";

    private final com.omnichat.websocket.security.JwtValidator jwtValidator;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String token = null;

        // 1. Try query parameter ?token=...
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("token");
            // Fallback for dev testing if they still use agentId
            if (token == null || token.isBlank()) {
                String devAgentId = servletRequest.getServletRequest().getParameter("agentId");
                if (devAgentId != null && !devAgentId.isBlank()) {
                    attributes.put(AGENT_ID_ATTR, devAgentId);
                    log.info("WebSocket handshake: DEV mode bypass with agentId={}", devAgentId);
                    return true;
                }
            }
        }

        // 2. Fallback: try Authorization header
        if (token == null || token.isBlank()) {
            var authHeaders = request.getHeaders().get("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String bearer = authHeaders.get(0);
                if (bearer != null && bearer.startsWith("Bearer ")) {
                    token = bearer.substring(7);
                }
            }
        }

        if (token != null && !token.isBlank()) {
            // Check Redis blacklist
            Boolean isBlacklisted = redisTemplate.hasKey("blacklist:" + token);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                log.warn("WebSocket handshake rejected: Token is blacklisted");
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }

            String userId = jwtValidator.validateTokenAndGetUserId(token);
            if (userId != null) {
                attributes.put(AGENT_ID_ATTR, userId);
                String tenantId = jwtValidator.getTenantIdFromToken(token);
                if (tenantId != null) {
                    attributes.put("tenantId", tenantId);
                }
                log.info("WebSocket handshake: agentId={} (tenantId={}) extracted from valid JWT", userId, tenantId);
                return true;
            }
        }

        log.warn("WebSocket handshake rejected: no valid token provided");
        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return false; // Reject connection
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
