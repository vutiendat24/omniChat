package com.omnichat.websocket.handler;

import com.omnichat.websocket.security.JwtValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentHandshakeInterceptorTest {

    @Mock
    private JwtValidator jwtValidator;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ServerHttpResponse response;

    @InjectMocks
    private AgentHandshakeInterceptor interceptor;

    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        attributes = new HashMap<>();
    }

    @Test
    void testBeforeHandshake_WithValidToken_ShouldAccept() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setParameter("token", "valid.jwt.token");
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        when(redisTemplate.hasKey("blacklist:valid.jwt.token")).thenReturn(false);
        when(jwtValidator.validateTokenAndGetUserId("valid.jwt.token")).thenReturn("user-1");

        boolean result = interceptor.beforeHandshake(request, response, null, attributes);

        assertTrue(result);
        assertEquals("user-1", attributes.get(AgentHandshakeInterceptor.AGENT_ID_ATTR));
    }

    @Test
    void testBeforeHandshake_WithBlacklistedToken_ShouldReject() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setParameter("token", "blacklisted.jwt.token");
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        when(redisTemplate.hasKey("blacklist:blacklisted.jwt.token")).thenReturn(true);

        boolean result = interceptor.beforeHandshake(request, response, null, attributes);

        assertFalse(result);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testBeforeHandshake_WithInvalidToken_ShouldReject() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setParameter("token", "invalid.jwt.token");
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        when(redisTemplate.hasKey("blacklist:invalid.jwt.token")).thenReturn(false);
        when(jwtValidator.validateTokenAndGetUserId("invalid.jwt.token")).thenReturn(null);

        boolean result = interceptor.beforeHandshake(request, response, null, attributes);

        assertFalse(result);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }
}
