package com.omnichat.integration.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.service.OutboundMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer that listens on omnichat.conversation.events topic
 * for agent-sent messages (conversation.message.received events with AGENT sender).
 *
 * When an agent sends a message in conversation-service, the event flows here
 * so integration-service can push it out to the external channel (Facebook Messenger).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationEventConsumer {

    private static final String TOPIC = "omnichat.conversation.events";

    private final OutboundMessageService outboundMessageService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC, groupId = "integration-service-group")
    public void consumeConversationEvent(Object eventPayload, Acknowledgment acknowledgment) {
        try {
            JsonNode event;
            if (eventPayload instanceof JsonNode) {
                event = (JsonNode) eventPayload;
            } else {
                event = objectMapper.valueToTree(eventPayload);
            }

            String eventType = event.path("eventType").asText("");

            // Only process "conversation.message.received" events
            // that need to be pushed to external channels
            if ("conversation.message.received".equals(eventType)) {
                processMessageReceivedEvent(event);
            } else {
                log.debug("Ignoring event type: {}", eventType);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process conversation event", e);
            throw new RuntimeException("Failed to process conversation event", e);
        }
    }

    private void processMessageReceivedEvent(JsonNode event) {
        String conversationId = event.path("conversationId").asText();
        String messageId = event.path("messageId").asText();
        String conversationStatus = event.path("conversationStatus").asText();

        // Extract additional fields needed for outbound delivery
        // These fields are populated by conversation-service when publishing the event
        String recipientExternalId = event.path("recipientExternalId").asText("");
        long channelConnectionId = event.path("channelConnectionId").asLong(0);
        String messageText = event.path("messageText").asText("");

        if (recipientExternalId.isEmpty() || channelConnectionId == 0) {
            log.warn("Missing recipientExternalId or channelConnectionId in event for conversation: {}. " +
                    "This may be a customer-originated message that doesn't need outbound push.", conversationId);
            return;
        }

        outboundMessageService.sendMessageToExternalChannel(
                conversationId, messageId, recipientExternalId,
                channelConnectionId, messageText, conversationStatus);
    }
}
