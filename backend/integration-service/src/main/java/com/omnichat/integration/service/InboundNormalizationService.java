package com.omnichat.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.dto.RawWebhookEvent;
import com.omnichat.integration.dto.unified.UnifiedMessage;
import com.omnichat.integration.service.parser.WebhookParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InboundNormalizationService {

    private static final Logger log = LoggerFactory.getLogger(InboundNormalizationService.class);

    private final List<WebhookParser> parsers;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InboundNormalizationService(List<WebhookParser> parsers, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.parsers = parsers;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "webhook_events_verified", groupId = "normalization_group")
    public void normalizeAndForward(String eventJson) {
        try {
            RawWebhookEvent event = objectMapper.readValue(eventJson, RawWebhookEvent.class);
            
            WebhookParser parser = parsers.stream()
                    .filter(p -> p.supports(event.getPlatform()))
                    .findFirst()
                    .orElse(null);

            if (parser == null) {
                log.warn("No parser found for platform: {}", event.getPlatform());
                return;
            }

            List<UnifiedMessage> unifiedMessages = parser.parse(event.getPayload());
            
            for (UnifiedMessage msg : unifiedMessages) {
                String msgJson = objectMapper.writeValueAsString(msg);
                kafkaTemplate.send("inbound_normalized_messages", msgJson);
                log.debug("Forwarded normalized message: {}", msgJson);
            }

        } catch (Exception e) {
            log.error("Failed to normalize webhook event", e);
        }
    }
}
