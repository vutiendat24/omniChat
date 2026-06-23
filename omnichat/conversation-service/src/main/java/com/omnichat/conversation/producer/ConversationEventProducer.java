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

    /**
     * Task 3.4.1.1 - Publish ConversationUpdated event after route assignment.
     *
     * Published when a conversation transitions from UNASSIGNED → OPEN with an assigned agent.
     * Consumed by WebSocket Service (Task 6.2.1.1) to push real-time updates to Agent UI.
     *
     * @param conversationId the updated conversation
     * @param assignedAgentId the agent now assigned to this conversation
     * @param status the new conversation status (e.g., "OPEN")
     */
    public void publishConversationUpdated(String conversationId, Long assignedAgentId, String status) {
        Map<String, Object> event = Map.of(
                "eventType", "conversation.updated",
                "conversationId", conversationId,
                "assignedAgentId", assignedAgentId,
                "status", status,
                "timestamp", LocalDateTime.now().toString()
        );

        kafkaTemplate.send(TOPIC, conversationId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published conversation.updated event: conversationId={}, agentId={}, status={}",
                                conversationId, assignedAgentId, status);
                    } else {
                        log.error("Failed to publish conversation.updated event for conversationId={}",
                                conversationId, ex);
                    }
                });
    }

    /**
     * Publish ConversationTransferred event for manual transfer (UC-303).
     *
     * Consumed by:
     * - Routing Service → adjusts workload: decrement old agent, increment new agent
     * - WebSocket Service → pushes transfer notification to both old and new agents
     *
     * @param conversationId the transferred conversation
     * @param fromAgentId    the agent releasing the conversation
     * @param toAgentId      the agent receiving the conversation
     * @param reason         optional reason for the transfer
     */
    public void publishConversationTransferred(String conversationId, Long fromAgentId, Long toAgentId, String reason) {
        java.util.HashMap<String, Object> event = new java.util.HashMap<>();
        event.put("eventType", "conversation.transferred");
        event.put("conversationId", conversationId);
        event.put("fromAgentId", fromAgentId);
        event.put("toAgentId", toAgentId);
        event.put("reason", reason != null ? reason : "");
        event.put("status", "OPEN");
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send(TOPIC, conversationId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published conversation.transferred event: conversationId={}, from={}, to={}",
                                conversationId, fromAgentId, toAgentId);
                    } else {
                        log.error("Failed to publish conversation.transferred event for conversationId={}",
                                conversationId, ex);
                    }
                });
    }
}
