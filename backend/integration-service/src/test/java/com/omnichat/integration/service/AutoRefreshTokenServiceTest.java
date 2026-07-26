package com.omnichat.integration.service;

import com.omnichat.integration.entity.ChannelConnection;
import com.omnichat.integration.repository.ChannelConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AutoRefreshTokenServiceTest {

    @Mock
    private ChannelConnectionRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AutoRefreshTokenService autoRefreshTokenService;

    private ChannelConnection expiringChannel;

    @BeforeEach
    void setUp() {
        expiringChannel = new ChannelConnection();
        expiringChannel.setId(1L);
        expiringChannel.setChannelId("page_123");
        expiringChannel.setPlatform(ChannelConnection.Platform.FACEBOOK);
        expiringChannel.setStatus(ChannelConnection.ConnectionStatus.CONNECTED);
        expiringChannel.setAccessToken("old_token");
        expiringChannel.setRefreshToken("old_refresh");
        expiringChannel.setExpiryDate(LocalDateTime.now().plusHours(10));
    }

    @Test
    void givenExpiringActiveChannels_whenRefresh_thenSuccessAndUpdateDB() {
        // Given
        when(repository.findByStatusAndExpiryDateBefore(eq(ChannelConnection.ConnectionStatus.CONNECTED), any(LocalDateTime.class)))
                .thenReturn(List.of(expiringChannel));
        
        Map<String, Object> mockResponse = Map.of(
                "access_token", "new_token",
                "expires_in", 3600
        );
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // When
        autoRefreshTokenService.refreshTokens();

        // Then
        assertEquals("new_token", expiringChannel.getAccessToken());
        assertEquals(ChannelConnection.ConnectionStatus.CONNECTED, expiringChannel.getStatus());
        verify(repository, times(1)).save(expiringChannel);
    }

    @Test
    void givenRefreshFailedWith4xx_thenStatusSetToError() {
        // Given
        when(repository.findByStatusAndExpiryDateBefore(eq(ChannelConnection.ConnectionStatus.CONNECTED), any(LocalDateTime.class)))
                .thenReturn(List.of(expiringChannel));
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid Token"));

        // When
        autoRefreshTokenService.refreshTokens();

        // Then
        assertEquals(ChannelConnection.ConnectionStatus.ERROR, expiringChannel.getStatus());
        verify(repository, times(1)).save(expiringChannel);
    }
}
