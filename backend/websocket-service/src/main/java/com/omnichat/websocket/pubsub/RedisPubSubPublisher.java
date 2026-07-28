package com.omnichat.websocket.pubsub;

import com.omnichat.websocket.config.RedisPubSubConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPubSubPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(String targetAgentId, String destination, Map<String, Object> payload) {
        SyncMessage message = SyncMessage.builder()
                .targetAgentId(targetAgentId)
                .destination(destination)
                .payload(payload)
                .sourceNodeId(RedisPubSubListener.NODE_ID)
                .build();

        redisTemplate.convertAndSend(RedisPubSubConfig.SYNC_TOPIC, message);
        log.info("Node {} published event to Redis Pub/Sub for agent {}", RedisPubSubListener.NODE_ID, targetAgentId);
    }
}
