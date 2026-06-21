package com.omnichat.conversation.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.conversation.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationEventConsumer {

    private static final String TOPIC = "omnichat.integration.events";

    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC, groupId = "${spring.application.name}-group")
    public void consumeIntegrationMessage(Object eventPayload, Acknowledgment acknowledgment) {
        try {
            log.info("Received IntegrationMessageReceived event: {}", eventPayload);

            // Convert the payload to JsonNode for processing
            JsonNode jsonPayload;
            if (eventPayload instanceof JsonNode) {
                jsonPayload = (JsonNode) eventPayload;
            } else {
                jsonPayload = objectMapper.valueToTree(eventPayload);
            }

            // Delegate to ConversationService for upsert + message save + event publish
            conversationService.processIncomingMessage(jsonPayload);

            // Manually acknowledge the message after successful processing
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process integration message event", e);
            // Throw exception so that DefaultErrorHandler can handle it (retry -> DLQ)
            throw new RuntimeException("Failed to process integration event", e);
        }
    }
}
