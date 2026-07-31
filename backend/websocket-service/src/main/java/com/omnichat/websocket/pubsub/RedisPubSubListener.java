package com.omnichat.websocket.pubsub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPubSubListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;
    
    // Generate a unique ID for this node at startup
    public static final String NODE_ID = UUID.randomUUID().toString();

    public void handleMessage(SyncMessage message) {
        // Prevent processing our own messages (Infinite loop prevention)
        if (NODE_ID.equals(message.getSourceNodeId())) {
            return;
        }

        String targetAgentId = message.getTargetAgentId();
        
        // Check local RAM (SimpUserRegistry) to see if user is connected here
        SimpUser user = simpUserRegistry.getUser(targetAgentId);
        if (user != null && user.hasSessions()) {
            // Push locally via WebSocket
            messagingTemplate.convertAndSendToUser(targetAgentId, message.getDestination(), message.getPayload());
            log.info("Node {} received from Redis and pushed to local WS for agent {}", NODE_ID, targetAgentId);
        } else {
            // "Các node nhận được từ Redis Pub/Sub tuyệt đối không được publish ngược lại vào Redis."
            // So we just ignore it.
            log.debug("Node {} received from Redis but agent {} not found locally. Ignoring.", NODE_ID, targetAgentId);
        }
    }
}
