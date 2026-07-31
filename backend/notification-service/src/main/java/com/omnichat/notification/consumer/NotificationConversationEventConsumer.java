package com.omnichat.notification.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConversationEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = "omnichat.conversation.events", groupId = "${spring.application.name}-conversation-group")
    public void consumeConversationEvent(Object message, Acknowledgment acknowledgment) {
        try {
            JsonNode event = toJsonNode(unwrapPayload(message));
            String eventType = event.path("eventType").asText("");

            if ("conversation.updated".equals(eventType)) {
                handleConversationUpdated(event);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process conversation event in notification service", e);
            // Even on error, we acknowledge to avoid poison pills, or we can throw
            acknowledgment.acknowledge();
        }
    }

    private void handleConversationUpdated(JsonNode event) {
        Long agentId = event.path("assignedAgentId").asLong(0);
        if (agentId == 0) return;
        
        String conversationId = event.path("conversationId").asText();
        String title = "New Conversation Assigned";
        String body = "Conversation " + conversationId + " has been assigned to you.";
        
        notificationService.createInAppNotification(agentId, "conversation.updated", title, body);
    }

    private Object unwrapPayload(Object message) {
        if (message instanceof org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record) {
            return record.value();
        }
        return message;
    }

    private JsonNode toJsonNode(Object eventPayload) throws java.io.IOException {
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
}
