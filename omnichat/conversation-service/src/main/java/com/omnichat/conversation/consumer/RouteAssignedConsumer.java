package com.omnichat.conversation.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.conversation.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Task 3.4.1.1 - Consume RouteAssigned event from Routing Service.
 *
 * Listens to "omnichat.conversation.events" topic for "route.assigned" events
 * published by RoutingEventProducer (Task 4.3.3.1).
 *
 * Flow (per Architecture_Design_OCM.md §5 Sequence Diagram):
 *   Kafka(route.assigned) → Conversation Service
 *     → Update Conversation(status=OPEN, assignedAgentId=X)
 *     → Publish ConversationUpdated event (for WebSocket Service)
 *
 * Event payload format (from RoutingEventProducer):
 * {
 *   "eventType": "route.assigned",
 *   "conversationId": "uuid-...",
 *   "agentId": 1,
 *   "timestamp": "2026-06-21T22:50:00"
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteAssignedConsumer {

    private static final String TOPIC = "omnichat.conversation.events";

    private final ConversationService conversationService;
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

            // Only handle route.assigned events; ignore all others
            // (other events on this topic like conversation.created, conversation.message.received
            //  are not relevant to this consumer)
            if (!"route.assigned".equals(eventType)) {
                acknowledgment.acknowledge();
                return;
            }

            String conversationId = event.path("conversationId").asText("");
            Long agentId = event.path("agentId").asLong(0);

            if (conversationId.isEmpty() || agentId == 0) {
                log.warn("Invalid route.assigned event: conversationId={}, agentId={}", conversationId, agentId);
                acknowledgment.acknowledge();
                return;
            }

            log.info("Received route.assigned event: conversationId={}, agentId={}", conversationId, agentId);

            // Delegate to ConversationService to update DB + publish ConversationUpdated
            conversationService.handleRouteAssigned(conversationId, agentId);

            // Manually acknowledge after successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process route.assigned event", e);
            // Rethrow so DefaultErrorHandler can retry → DLQ
            throw new RuntimeException("Failed to process route.assigned event", e);
        }
    }
}
