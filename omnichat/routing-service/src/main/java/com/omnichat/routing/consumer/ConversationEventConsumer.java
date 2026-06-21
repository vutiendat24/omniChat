package com.omnichat.routing.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.routing.service.RoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Task 4.3.1.1 - Consume ConversationMessageReceived / ConversationCreated events.
 *
 * Listens to topic "omnichat.conversation.events" and filters for events
 * that indicate an UNASSIGNED conversation requiring agent assignment.
 *
 * Relevant event types:
 * - "conversation.created"           → New conversation, always UNASSIGNED → trigger routing
 * - "conversation.message.received"  → Only trigger routing if conversationStatus == "UNASSIGNED"
 *
 * Flow (per Architecture_Design_OCM.md §4 Data Flow):
 *   Kafka → Routing Service → Check if already assigned?
 *     YES → Skip (do not re-route)
 *     NO  → Run round-robin algorithm → Publish RouteAssigned
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationEventConsumer {

    private static final String TOPIC = "omnichat.conversation.events";

    private final RoutingService routingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC, groupId = "${spring.application.name}-group")
    public void consumeConversationEvent(Object eventPayload, Acknowledgment acknowledgment) {
        try {
            // Convert payload to JsonNode for flexible field access
            JsonNode event;
            if (eventPayload instanceof JsonNode) {
                event = (JsonNode) eventPayload;
            } else {
                event = objectMapper.valueToTree(eventPayload);
            }

            String eventType = event.path("eventType").asText("");
            String conversationId = event.path("conversationId").asText("");

            log.info("Received event: type={}, conversationId={}", eventType, conversationId);

            switch (eventType) {
                case "conversation.created" -> {
                    // New conversation is always UNASSIGNED → trigger routing
                    String status = event.path("status").asText("UNASSIGNED");
                    if ("UNASSIGNED".equals(status)) {
                        log.info("New UNASSIGNED conversation detected: {}, triggering routing", conversationId);
                        routingService.routeConversation(conversationId);
                    } else {
                        log.debug("Conversation {} created with status {}, skipping routing", conversationId, status);
                    }
                }

                case "conversation.message.received" -> {
                    // Only route if the conversation is still UNASSIGNED
                    String conversationStatus = event.path("conversationStatus").asText("");
                    if ("UNASSIGNED".equals(conversationStatus)) {
                        log.info("Message received for UNASSIGNED conversation: {}, triggering routing", conversationId);
                        routingService.routeConversation(conversationId);
                    } else {
                        log.debug("Message received for already-assigned conversation: {} (status={}), skipping",
                                conversationId, conversationStatus);
                    }
                }

                default -> log.debug("Ignoring event type '{}' for conversationId={}", eventType, conversationId);
            }

            // Manually acknowledge after successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process conversation event: {}", e.getMessage(), e);
            // Rethrow so DefaultErrorHandler can retry → DLQ
            throw new RuntimeException("Failed to process conversation event", e);
        }
    }
}
