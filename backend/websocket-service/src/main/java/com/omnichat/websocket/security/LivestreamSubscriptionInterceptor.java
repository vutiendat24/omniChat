package com.omnichat.websocket.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class LivestreamSubscriptionInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/livestream/")) {
                // Expected format: /topic/livestream/{tenantId}/{roomId}
                String[] parts = destination.split("/");
                if (parts.length >= 5) {
                    String targetTenantId = parts[3]; // "", "topic", "livestream", "{tenantId}", "{roomId}"
                    
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null) {
                        String sessionTenantId = (String) sessionAttributes.get("tenantId");
                        
                        if (sessionTenantId == null || !sessionTenantId.equals(targetTenantId)) {
                            log.warn("Unauthorized subscribe attempt to {} by tenant {}", destination, sessionTenantId);
                            throw new IllegalArgumentException("Unauthorized to subscribe to this room");
                        }
                    } else {
                        log.warn("No session attributes found for subscribe to {}", destination);
                        throw new IllegalArgumentException("Unauthorized to subscribe to this room");
                    }
                }
            }
        }
        return message;
    }
}
