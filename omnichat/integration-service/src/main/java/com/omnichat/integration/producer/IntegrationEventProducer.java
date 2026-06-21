package com.omnichat.integration.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "omnichat.integration.events";

    public void publishIntegrationMessageReceived(String externalConversationId, Object eventPayload) {
        // Publish event to Kafka using externalConversationId as the message key to ensure ordering
        kafkaTemplate.send(TOPIC, externalConversationId, eventPayload)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Message sent to topic {} with key {}", TOPIC, externalConversationId);
                    } else {
                        log.error("Failed to send message to topic {}", TOPIC, ex);
                    }
                });
    }
}
