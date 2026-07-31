package com.omnichat.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    /**
     * Resolves the key for Rate Limiting. 
     * Here we try to use X-User-Id header if present (set by JwtAuthenticationFilter),
     * otherwise fallback to the IP Address.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }
            
            String ip = exchange.getRequest().getRemoteAddress() != null ? 
                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
            return Mono.just(ip);
        };
    }
}
