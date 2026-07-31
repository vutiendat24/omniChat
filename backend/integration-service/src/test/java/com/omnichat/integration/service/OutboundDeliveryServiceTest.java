package com.omnichat.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.dto.unified.UnifiedMessage;
import com.omnichat.integration.entity.ChannelConnection;
import com.omnichat.integration.repository.ChannelConnectionRepository;
import com.omnichat.integration.service.builder.MessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboundDeliveryServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ChannelConnectionRepository repository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private MessageBuilder mockBuilder;

    @InjectMocks
    private OutboundDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new OutboundDeliveryService(List.of(mockBuilder), kafkaTemplate, objectMapper, repository, restTemplate);
    }

    @Test
    void givenValidMessage_whenSend_thenSuccess() throws Exception {
        String msgJson = "{\"platform\":\"FACEBOOK\",\"channelId\":\"page_1\",\"messageType\":\"TEXT\"}";
        UnifiedMessage msg = UnifiedMessage.builder().platform("FACEBOOK").channelId("page_1").messageType(UnifiedMessage.MessageType.TEXT).build();
        
        when(objectMapper.readValue(msgJson, UnifiedMessage.class)).thenReturn(msg);
        when(mockBuilder.supports("FACEBOOK")).thenReturn(true);
        when(mockBuilder.buildPayload(msg)).thenReturn(Map.of("test", "data"));
        
        ChannelConnection channel = new ChannelConnection();
        channel.setAccessToken("token123");
        when(repository.findByChannelIdAndPlatform("page_1", ChannelConnection.Platform.FACEBOOK)).thenReturn(Optional.of(channel));
        
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("message_id", "msg_123")));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":\"SENT\"}");

        service.deliverMessage(msgJson);

        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
        verify(kafkaTemplate, times(1)).send(eq("message_delivery_status"), argThat(arg -> arg.contains("SENT")));
    }

    @Test
    void givenPolicyError_whenSend_thenFailedStatus() throws Exception {
        String msgJson = "{\"platform\":\"FACEBOOK\",\"channelId\":\"page_1\",\"messageType\":\"TEXT\"}";
        UnifiedMessage msg = UnifiedMessage.builder().platform("FACEBOOK").channelId("page_1").messageType(UnifiedMessage.MessageType.TEXT).build();
        
        when(objectMapper.readValue(msgJson, UnifiedMessage.class)).thenReturn(msg);
        when(mockBuilder.supports("FACEBOOK")).thenReturn(true);
        when(mockBuilder.buildPayload(msg)).thenReturn(Map.of("test", "data"));
        
        ChannelConnection channel = new ChannelConnection();
        channel.setAccessToken("token123");
        when(repository.findByChannelIdAndPlatform("page_1", ChannelConnection.Platform.FACEBOOK)).thenReturn(Optional.of(channel));
        
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(org.springframework.http.HttpStatus.BAD_REQUEST, "Policy violation error 10"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":\"FAILED\"}");

        service.deliverMessage(msgJson);

        verify(kafkaTemplate, times(1)).send(eq("message_delivery_status"), argThat(arg -> arg.contains("FAILED")));
    }
}
