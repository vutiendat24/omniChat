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
    private final com.omnichat.websocket.handler.AgentHandshakeHandler agentHandshakeHandler;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for /topic (broadcast) and /queue (point-to-point)
        org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler taskScheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-thread-");
        taskScheduler.initialize();

        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[]{60000, 60000}) // 60 seconds heartbeat
              .setTaskScheduler(taskScheduler);

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
                .setHandshakeHandler(agentHandshakeHandler)
                .setAllowedOriginPatterns("*")  // Allow all origins in dev (restrict in prod)
                .withSockJS();

        // Also register without SockJS for native WebSocket clients (e.g., Postman)
        registry.addEndpoint("/ws")
                .addInterceptors(agentHandshakeInterceptor)
                .setHandshakeHandler(agentHandshakeHandler)
                .setAllowedOriginPatterns("*");
    }
}
