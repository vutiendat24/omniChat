package com.omnichat.integration.service;

import com.omnichat.integration.entity.ChannelConnection.Platform;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class RedisStateService {

    private final StringRedisTemplate redisTemplate;
    private static final String STATE_PREFIX = "oauth:state:";
    private static final String PLATFORM_PREFIX = "oauth:platform:";

    public RedisStateService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateState(String tenantId, Platform platform) {
        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(STATE_PREFIX + state, tenantId, Duration.ofMinutes(15));
        redisTemplate.opsForValue().set(PLATFORM_PREFIX + state, platform.name(), Duration.ofMinutes(15));
        return state;
    }

    public static class OAuthStateData {
        public String tenantId;
        public Platform platform;
        public OAuthStateData(String tenantId, Platform platform) {
            this.tenantId = tenantId;
            this.platform = platform;
        }
    }

    public Optional<OAuthStateData> validateAndGetStateData(String state) {
        String tenantId = redisTemplate.opsForValue().get(STATE_PREFIX + state);
        String platformStr = redisTemplate.opsForValue().get(PLATFORM_PREFIX + state);
        
        if (tenantId != null && platformStr != null) {
            // Delete state after one-time use to prevent replay attacks
            redisTemplate.delete(STATE_PREFIX + state);
            redisTemplate.delete(PLATFORM_PREFIX + state);
            return Optional.of(new OAuthStateData(tenantId, Platform.valueOf(platformStr)));
        }
        return Optional.empty();
    }
}
