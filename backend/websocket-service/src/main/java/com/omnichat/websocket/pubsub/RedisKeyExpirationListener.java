package com.omnichat.websocket.pubsub;

import com.omnichat.websocket.session.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    private final WebSocketSessionManager sessionManager;

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer,
                                      WebSocketSessionManager sessionManager) {
        super(listenerContainer);
        this.sessionManager = sessionManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        if (expiredKey.startsWith(WebSocketSessionManager.SESSION_KEY_PREFIX)) {
            String agentId = expiredKey.substring(WebSocketSessionManager.SESSION_KEY_PREFIX.length());
            log.warn("Detected Redis key expiration for agentId={}. Node might have crashed without disconnecting.", agentId);
            
            // Publish OFFLINE event since the agent's TTL expired (no heartbeat for 3 mins)
            sessionManager.publishPresenceEvent(agentId, com.omnichat.websocket.event.AgentPresenceEvent.PresenceStatus.OFFLINE);
        }
    }
}
