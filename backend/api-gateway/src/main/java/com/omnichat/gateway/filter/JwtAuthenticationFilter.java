package com.omnichat.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final PublicKey publicKey;
    private final ReactiveStringRedisTemplate redisTemplate;
    
    // Define public routes that do not require authentication
    private final List<String> openEndpoints = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/actuator"
    );

    public JwtAuthenticationFilter(@Value("${jwt.public-key}") String publicKeyString,
                                   ReactiveStringRedisTemplate redisTemplate) 
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.publicKey = loadPublicKey(publicKeyString);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Skip authentication for open endpoints
        if (openEndpoints.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 2. Check for Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthenticated(exchange, "Missing or invalid Authorization header");
        }

        // 3. Extract Token
        String token = authHeader.substring(7);

        // 4. Check Blacklist in Redis and then Verify Token
        return redisTemplate.hasKey("blacklist:" + token)
                .flatMap(isBlacklisted -> {
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        return unauthenticated(exchange, "Token has been revoked or logged out");
                    }

                    try {
                        // Verify signature and expiration
                        Claims claims = Jwts.parser()
                                .verifyWith(publicKey)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();

                        // Extract Claims
                        String userId = claims.getSubject();
                        String roles = claims.get("roles", String.class);

                        // Mutate request to add headers
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", userId != null ? userId : "")
                                .header("X-User-Roles", roles != null ? roles : "")
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    } catch (JwtException | IllegalArgumentException e) {
                        return unauthenticated(exchange, "Invalid JWT Token: " + e.getMessage());
                    }
                });
    }

    private Mono<Void> unauthenticated(ServerWebExchange exchange, String message) {
        return Mono.error(new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, message));
    }

    @Override
    public int getOrder() {
        return -1; // Execute early in the filter chain
    }

    private PublicKey loadPublicKey(String keyStr) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (keyStr == null || keyStr.isEmpty()) {
            throw new IllegalArgumentException("JWT Public Key is not configured");
        }
        
        String publicKeyContent = keyStr
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(publicKeyContent);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
}
