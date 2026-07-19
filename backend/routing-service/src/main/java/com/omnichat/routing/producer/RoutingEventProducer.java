package com.omnichat.routing.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Task 4.3.3.1 - Publish RouteAssigned event to Kafka.
 *
 * Publishes to "omnichat.conversation.events" topic with conversationId as the partition key,
 * ensuring ordering guarantee per conversation (same partition as other conversation events).
 *
 * Consumed by:
 * - Conversation Service → updates Conversation status to OPEN and sets assignedAgentId
 * - WebSocket Service    → pushes real-time assignment notification to Agent UI
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "omnichat.conversation.events";

    /**
     * Publish RouteAssigned event after the routing algorithm selects an agent.
     *
     * @param conversationId the conversation being assigned
     * @param agentId        the selected agent
     */
    public void publishRouteAssigned(String conversationId, Long agentId) {
        Map<String, Object> event = Map.of(
                "eventType", "route.assigned",
                "conversationId", conversationId,
                "agentId", agentId,
                "timestamp", LocalDateTime.now().toString()
        );

        kafkaTemplate.send(TOPIC, conversationId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published route.assigned event: conversationId={}, agentId={}",
                                conversationId, agentId);
                    } else {
                        log.error("Failed to publish route.assigned event: conversationId={}, agentId={}",
                                conversationId, agentId, ex);
                    }
                });
    }
}
