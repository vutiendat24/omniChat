package com.omnichat.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.dto.RawWebhookEvent;
import com.omnichat.integration.dto.unified.UnifiedMessage;
import com.omnichat.integration.service.parser.WebhookParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InboundNormalizationServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebhookParser mockParser;

    @InjectMocks
    private InboundNormalizationService service;

    @BeforeEach
    void setUp() {
        service = new InboundNormalizationService(List.of(mockParser), kafkaTemplate, objectMapper);
    }

    @Test
    void testNormalizeAndForward() throws Exception {
        String eventJson = "{\"platform\":\"FACEBOOK\",\"payload\":\"{}\"}";
        RawWebhookEvent event = new RawWebhookEvent("FACEBOOK", "sig", "{}");
        
        when(objectMapper.readValue(eventJson, RawWebhookEvent.class)).thenReturn(event);
        when(mockParser.supports("FACEBOOK")).thenReturn(true);
        
        UnifiedMessage um = UnifiedMessage.builder().platform("FACEBOOK").build();
        when(mockParser.parse("{}")).thenReturn(Collections.singletonList(um));
        when(objectMapper.writeValueAsString(um)).thenReturn("{\"platform\":\"FACEBOOK\"}");

        service.normalizeAndForward(eventJson);

        verify(kafkaTemplate, times(1)).send(eq("inbound_normalized_messages"), eq("{\"platform\":\"FACEBOOK\"}"));
    }

    @Test
    void testNoParserFound() throws Exception {
        String eventJson = "{\"platform\":\"UNKNOWN\",\"payload\":\"{}\"}";
        RawWebhookEvent event = new RawWebhookEvent("UNKNOWN", "sig", "{}");
        
        when(objectMapper.readValue(eventJson, RawWebhookEvent.class)).thenReturn(event);
        when(mockParser.supports("UNKNOWN")).thenReturn(false);

        service.normalizeAndForward(eventJson);

        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }
}
