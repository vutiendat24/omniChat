package com.omnichat.conversation.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "omnichat.conversation.events";

    public void publishConversationCreated(String conversationId, String channelIdentityId, Long channelConnectionId) {
        Map<String, Object> event = Map.of(
                "eventType", "conversation.created",
                "conversationId", conversationId,
                "channelIdentityId", channelIdentityId,
                "channelConnectionId", channelConnectionId,
                "status", "UNASSIGNED",
                "timestamp", LocalDateTime.now().toString()
        );

        kafkaTemplate.send(TOPIC, conversationId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published conversation.created event for conversationId={}", conversationId);
                    } else {
                        log.error("Failed to publish conversation.created event for conversationId={}", conversationId, ex);
                    }
                });
    }

    public void publishConversationMessageReceived(
            String conversationId, String messageId, String status,
            String recipientExternalId, Long channelConnectionId, String messageText) {

        java.util.HashMap<String, Object> event = new java.util.HashMap<>();
        event.put("eventType", "conversation.message.received");
        event.put("conversationId", conversationId);
        event.put("messageId", messageId);
        event.put("conversationStatus", status);
        event.put("recipientExternalId", recipientExternalId != null ? recipientExternalId : "");
        event.put("channelConnectionId", channelConnectionId != null ? channelConnectionId : 0L);
        event.put("messageText", messageText != null ? messageText : "");
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send(TOPIC, conversationId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published conversation.message.received event for conversationId={}, status={}", conversationId, status);
                    } else {
                        log.error("Failed to publish conversation.message.received event for conversationId={}", conversationId, ex);
                    }
                });
    }
}
