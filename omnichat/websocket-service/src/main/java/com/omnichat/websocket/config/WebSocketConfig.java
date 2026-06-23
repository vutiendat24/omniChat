package com.omnichat.websocket.config;

import com.omnichat.websocket.handler.AgentHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Task 6.1.1.1 - Spring WebSocket Configuration
 *
 * Sets up STOMP over WebSocket for real-time Agent UI communication.
 *
 * Endpoints:
 * - /ws                → STOMP handshake endpoint (with SockJS fallback)
 *
 * Broker destinations:
 * - /topic/...         → Broadcast to all subscribers (e.g., /topic/conversations)
 * - /queue/...         → Point-to-point to a specific user (e.g., /queue/agent/messages)
 * - /app/...           → Application-level messages from client → server
 *
 * Handshake Interceptor:
 * - Extracts agentId from query param or header during STOMP CONNECT
 * - Stores agentId in WebSocket session attributes for session management
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AgentHandshakeInterceptor agentHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for /topic (broadcast) and /queue (point-to-point)
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix for client-to-server messages (e.g., /app/send)
        config.setApplicationDestinationPrefixes("/app");

        // User destination prefix for per-user messaging (e.g., /user/queue/notifications)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .addInterceptors(agentHandshakeInterceptor)
                .setAllowedOriginPatterns("*")  // Allow all origins in dev (restrict in prod)
                .withSockJS();

        // Also register without SockJS for native WebSocket clients (e.g., Postman)
        registry.addEndpoint("/ws")
                .addInterceptors(agentHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
