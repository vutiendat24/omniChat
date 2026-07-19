package com.omnichat.conversation.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.conversation.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationEventConsumer {

    private static final String TOPIC = "omnichat.integration.events";

    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC, groupId = "${spring.application.name}-group")
    public void consumeIntegrationMessage(Object message, Acknowledgment acknowledgment) {
        try {
            Object eventPayload = unwrapPayload(message);
            JsonNode jsonPayload = toJsonNode(eventPayload);

            if (!isInboundMessageEvent(jsonPayload) && !isFacebookMessageWebhook(jsonPayload)) {
                String eventType = jsonPayload.path("eventType").asText("unknown");
                log.info("Ignoring integration event not handled by conversation-service: eventType={}", eventType);
                acknowledgment.acknowledge();
                return;
            }

            log.info("Received integration message event");
            conversationService.processIncomingMessage(jsonPayload);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process integration message event", e);
            // Throw exception so that DefaultErrorHandler can handle it (retry -> DLQ)
            throw new RuntimeException("Failed to process integration event", e);
        }
    }

    private Object unwrapPayload(Object message) {
        if (message instanceof ConsumerRecord<?, ?> record) {
            log.info("Received integration Kafka record: key={}, partition={}, offset={}",
                    record.key(), record.partition(), record.offset());
            return record.value();
        }
        return message;
    }

    private JsonNode toJsonNode(Object eventPayload) throws IOException {
        if (eventPayload instanceof JsonNode jsonNode) {
            return jsonNode;
        }
        if (eventPayload instanceof String json) {
            return objectMapper.readTree(json);
        }
        if (eventPayload instanceof byte[] bytes) {
            return objectMapper.readTree(bytes);
        }
        return objectMapper.valueToTree(eventPayload);
    }

    private boolean isFacebookMessageWebhook(JsonNode jsonPayload) {
        JsonNode entry = jsonPayload.path("entry");
        if (!entry.isArray() || entry.isEmpty()) {
            return false;
        }

        JsonNode messaging = entry.get(0).path("messaging");
        return messaging.isArray()
                && !messaging.isEmpty()
                && messaging.get(0).has("message");
    }

    private boolean isInboundMessageEvent(JsonNode jsonPayload) {
        return "integration.message.received".equals(jsonPayload.path("eventType").asText(""));
    }
}
