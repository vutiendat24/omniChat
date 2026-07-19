package com.omnichat.integration.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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

    public void publishInboundMessageReceived(
            String platform,
            String externalUserId,
            Long channelConnectionId,
            String messageId,
            String messageText,
            Object rawPayload) {

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "integration.message.received");
        event.put("platform", platform);
        event.put("externalUserId", externalUserId);
        event.put("channelConnectionId", channelConnectionId);
        event.put("messageId", messageId);
        event.put("messageText", messageText != null ? messageText : "");
        event.put("rawPayload", rawPayload);
        event.put("timestamp", LocalDateTime.now().toString());

        publishIntegrationMessageReceived(platform + ":" + externalUserId, event);
    }
}
