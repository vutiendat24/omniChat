package com.omnichat.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "a-very-secure-secret-key-that-is-at-least-32-bytes-long-for-hmac-sha-256";
    private final String issuer = "omnichat-auth-service";
    private final String audience = "omnichat-clients";
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, issuer, audience);
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testValidToken() {
        String token = Jwts.builder()
                .subject("testuser")
                .issuer(issuer)
                .audience().add(audience).and()
                .expiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(key)
                .compact();

        assertTrue(jwtTokenProvider.validateToken(token));
        
        Authentication auth = jwtTokenProvider.getAuthentication(token);
        assertNotNull(auth);
        assertEquals("testuser", auth.getPrincipal());
    }

    @Test
    void testExpiredToken() {
        String token = Jwts.builder()
                .subject("testuser")
                .issuer(issuer)
                .audience().add(audience).and()
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // -1 hour
                .signWith(key)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testInvalidSignature() {
        SecretKey otherKey = Keys.hmacShaKeyFor("another-very-secure-secret-key-that-is-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("testuser")
                .issuer(issuer)
                .audience().add(audience).and()
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(otherKey)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(token));
    }
    
    @Test
    void testInvalidIssuerOrAudience() {
        String token = Jwts.builder()
                .subject("testuser")
                .issuer("wrong-issuer")
                .audience().add("wrong-audience").and()
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(token));
    }
}
