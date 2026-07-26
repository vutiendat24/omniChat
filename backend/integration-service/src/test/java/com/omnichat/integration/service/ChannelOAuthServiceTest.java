package com.omnichat.integration.service;

import com.omnichat.integration.entity.ChannelConnection;
import com.omnichat.integration.repository.ChannelConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChannelOAuthServiceTest {

    @Mock
    private ChannelConnectionRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisStateService redisStateService;

    @InjectMocks
    private ChannelOAuthServiceImpl channelOAuthService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void givenValidFacebookCallback_whenProcessCallback_thenStoreAndReturnSuccess() {
        // Given
        String code = "valid_code";
        String state = "valid_state";
        String tenantId = "tenant-1";
        
        RedisStateService.OAuthStateData stateData = new RedisStateService.OAuthStateData(tenantId, ChannelConnection.Platform.FACEBOOK);
        when(redisStateService.validateAndGetStateData(state)).thenReturn(Optional.of(stateData));
        
        // Mock token API
        java.util.Map<String, Object> tokenResponse = new java.util.HashMap<>();
        tokenResponse.put("access_token", "user_access_token");
        when(restTemplate.getForEntity(contains("oauth/access_token"), eq(java.util.Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(tokenResponse));
                
        // Mock pages API
        java.util.Map<String, Object> pageData = new java.util.HashMap<>();
        pageData.put("id", "page_123");
        pageData.put("name", "Test Page");
        pageData.put("access_token", "page_access_token");
        
        java.util.Map<String, Object> pagesResponse = new java.util.HashMap<>();
        pagesResponse.put("data", java.util.Collections.singletonList(pageData));
        when(restTemplate.getForEntity(contains("me/accounts"), eq(java.util.Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(pagesResponse));
        
        when(repository.findByChannelIdAndPlatform("page_123", ChannelConnection.Platform.FACEBOOK))
                .thenReturn(Optional.empty());
        when(repository.findByChannelIdAndTenantId("page_123", tenantId))
                .thenReturn(Optional.empty());
                
        when(repository.save(any(ChannelConnection.class))).thenAnswer(i -> {
            ChannelConnection conn = i.getArgument(0);
            conn.setId(1L);
            return conn;
        });

        // When
        assertDoesNotThrow(() -> channelOAuthService.handleCallback(code, state, null));
        
        // Then
        verify(repository, times(1)).save(any(ChannelConnection.class));
    }
    
    @Test
    void givenDuplicateChannel_whenProcessCallback_thenThrowException() {
        // Given
        String code = "valid_code";
        String state = "valid_state";
        String tenantId = "tenant-2";
        
        RedisStateService.OAuthStateData stateData = new RedisStateService.OAuthStateData(tenantId, ChannelConnection.Platform.FACEBOOK);
        when(redisStateService.validateAndGetStateData(state)).thenReturn(Optional.of(stateData));
        
        // Mock token API
        java.util.Map<String, Object> tokenResponse = new java.util.HashMap<>();
        tokenResponse.put("access_token", "user_access_token");
        when(restTemplate.getForEntity(contains("oauth/access_token"), eq(java.util.Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(tokenResponse));
                
        // Mock pages API
        java.util.Map<String, Object> pageData = new java.util.HashMap<>();
        pageData.put("id", "page_123");
        pageData.put("name", "Test Page");
        pageData.put("access_token", "page_access_token");
        
        java.util.Map<String, Object> pagesResponse = new java.util.HashMap<>();
        pagesResponse.put("data", java.util.Collections.singletonList(pageData));
        when(restTemplate.getForEntity(contains("me/accounts"), eq(java.util.Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(pagesResponse));
        
        ChannelConnection existingChannel = new ChannelConnection();
        existingChannel.setTenantId("tenant-1");
        when(repository.findByChannelIdAndPlatform("page_123", ChannelConnection.Platform.FACEBOOK))
                .thenReturn(Optional.of(existingChannel));
                
        // When & Then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> channelOAuthService.handleCallback(code, state, null));
        assertEquals("Channel already connected to another shop", ex.getMessage());
    }
    
    @Test
    void givenUserDenied_whenProcessCallback_thenThrowException() {
        // When & Then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> channelOAuthService.handleCallback(null, "state123", "access_denied"));
        assertTrue(ex.getMessage().contains("User denied access"));
    }

}
