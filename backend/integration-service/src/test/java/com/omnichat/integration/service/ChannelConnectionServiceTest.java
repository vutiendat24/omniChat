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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChannelConnectionServiceTest {

    @Mock
    private ChannelConnectionRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ChannelConnectionServiceImpl channelConnectionService;

    private ChannelConnection activeChannel;

    @BeforeEach
    void setUp() {
        activeChannel = new ChannelConnection();
        activeChannel.setId(1L);
        activeChannel.setChannelId("page_123");
        activeChannel.setPlatform(ChannelConnection.Platform.FACEBOOK);
        activeChannel.setStatus(ChannelConnection.ConnectionStatus.CONNECTED);
        activeChannel.setAccessToken("some_token");
        activeChannel.setRefreshToken("some_refresh");
    }

    @Test
    void givenActiveChannel_whenDisconnect_thenStatusInactiveAndTokenCleared() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(activeChannel));

        // When
        channelConnectionService.disconnectChannel(1L);

        // Then
        assertEquals(ChannelConnection.ConnectionStatus.DISCONNECTED, activeChannel.getStatus());
        assertNull(activeChannel.getAccessToken());
        assertNull(activeChannel.getRefreshToken());
        verify(repository, times(1)).save(activeChannel);
    }

    @Test
    void givenPlatformApiError_whenDisconnect_thenStillProceedAndDisconnectLocally() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(activeChannel));
        // Simulate platform API throw exception
        doThrow(new RuntimeException("Platform API Error"))
                .when(restTemplate).delete(anyString());

        // When
        assertDoesNotThrow(() -> channelConnectionService.disconnectChannel(1L));

        // Then
        assertEquals(ChannelConnection.ConnectionStatus.DISCONNECTED, activeChannel.getStatus());
        assertNull(activeChannel.getAccessToken());
        verify(repository, times(1)).save(activeChannel);
    }

    @Test
    void givenInactiveChannel_whenDisconnect_thenThrowException() {
        // Given
        activeChannel.setStatus(ChannelConnection.ConnectionStatus.DISCONNECTED);
        when(repository.findById(1L)).thenReturn(Optional.of(activeChannel));

        // When & Then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> channelConnectionService.disconnectChannel(1L));
        assertEquals("Channel is not connected", ex.getMessage());
        verify(repository, never()).save(any());
    }
}
