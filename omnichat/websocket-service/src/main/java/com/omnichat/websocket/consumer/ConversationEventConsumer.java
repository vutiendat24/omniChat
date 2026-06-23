package com.omnichat.websocket.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.websocket.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Task 6.2.1.1 - Kafka → WebSocket Bridge
 *
 * Consumes events from "omnichat.conversation.events" topic and pushes
 * them to connected Agent UIs via WebSocket (STOMP).
 *
 * Per Architecture_Design_OCM.md §5 Sequence Diagram:
 *   Kafka → WS Service → Tra cứu Connection WS của Agent X từ Redis
 *                       → Đẩy dữ liệu qua WebSocket (Real-time update UI)
 *
 * Event types handled:
 * - "conversation.updated"          → Push to assigned agent (route assignment notification)
 * - "conversation.message.received" → Push to assigned agent (new message notification)
 * - "conversation.created"          → Broadcast to all online agents (new conversation alert)
 * - "conversation.transferred"      → Push to both old and new agents (UC-303 transfer notification)
 *
 * STOMP destinations:
 * - /user/{agentId}/queue/conversations  → Per-agent targeted messages
 * - /topic/conversations                  → Broadcast to all agents
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationEventConsumer {

    private static final String TOPIC = "omnichat.conversation.events";

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC, groupId = "${spring.application.name}-group")
    public void consumeConversationEvent(Object eventPayload, Acknowledgment acknowledgment) {
        try {
            JsonNode event;
            if (eventPayload instanceof JsonNode) {
                event = (JsonNode) eventPayload;
            } else {
                event = objectMapper.valueToTree(eventPayload);
            }

            String eventType = event.path("eventType").asText("");
            String conversationId = event.path("conversationId").asText("");

            log.debug("WS Bridge received event: type={}, conversationId={}", eventType, conversationId);

            switch (eventType) {
                case "conversation.updated" -> handleConversationUpdated(event);
                case "conversation.message.received" -> handleMessageReceived(event);
                case "conversation.created" -> handleConversationCreated(event);
                case "conversation.transferred" -> handleConversationTransferred(event);
                default -> log.debug("WS Bridge ignoring event type: {}", eventType);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process event in WS Bridge", e);
            throw new RuntimeException("Failed to process event in WS Bridge", e);
        }
    }

    /**
     * Handle conversation.updated (route assignment) → push to the newly assigned agent.
     *
     * Payload:
     * { "eventType":"conversation.updated", "conversationId":"...", "assignedAgentId":1, "status":"OPEN" }
     */
    private void handleConversationUpdated(JsonNode event) {
        String conversationId = event.path("conversationId").asText();
        String agentId = String.valueOf(event.path("assignedAgentId").asLong(0));

        if ("0".equals(agentId)) {
            log.warn("conversation.updated event has no assignedAgentId, skipping WS push");
            return;
        }

        // Check if agent is connected via WebSocket
        if (!sessionManager.isAgentConnected(agentId)) {
            log.debug("Agent {} not connected via WS, skipping push for conversation {}", agentId, conversationId);
            return;
        }

        Map<String, Object> payload = buildPayload(event, "CONVERSATION_ASSIGNED");

        // Push to the specific agent's queue
        String destination = "/queue/conversations";
        messagingTemplate.convertAndSendToUser(agentId, destination, payload);

        log.info("Pushed conversation.updated to agent {} via WS: conversationId={}", agentId, conversationId);
    }

    /**
     * Handle conversation.message.received → push to the assigned agent (if known).
     *
     * Payload:
     * { "eventType":"conversation.message.received", "conversationId":"...",
     *   "conversationStatus":"OPEN", "messageId":"..." }
     */
    private void handleMessageReceived(JsonNode event) {
        String conversationId = event.path("conversationId").asText();
        String conversationStatus = event.path("conversationStatus").asText("");

        // For UNASSIGNED conversations, broadcast to all agents (new conversation notification)
        if ("UNASSIGNED".equals(conversationStatus)) {
            Map<String, Object> payload = buildPayload(event, "NEW_MESSAGE_UNASSIGNED");
            messagingTemplate.convertAndSend("/topic/conversations", payload);
            log.info("Broadcast unassigned message to /topic/conversations: conversationId={}", conversationId);
            return;
        }

        // For assigned conversations, try to push to the specific agent
        // Note: the event doesn't always carry agentId, so we broadcast to topic
        // and let the frontend filter by conversationId
        Map<String, Object> payload = buildPayload(event, "NEW_MESSAGE");
        messagingTemplate.convertAndSend("/topic/conversations", payload);

        log.info("Broadcast new message to /topic/conversations: conversationId={}, status={}",
                conversationId, conversationStatus);
    }

    /**
     * Handle conversation.created → broadcast to all agents (new queue item).
     */
    private void handleConversationCreated(JsonNode event) {
        String conversationId = event.path("conversationId").asText();

        Map<String, Object> payload = buildPayload(event, "NEW_CONVERSATION");
        messagingTemplate.convertAndSend("/topic/conversations", payload);

        log.info("Broadcast new conversation to /topic/conversations: conversationId={}", conversationId);
    }

    /**
     * UC-303: Handle conversation.transferred → notify both old and new agents.
     *
     * Payload:
     * { "eventType":"conversation.transferred", "conversationId":"...",
     *   "fromAgentId":1, "toAgentId":2, "reason":"...", "status":"OPEN" }
     *
     * Pushes:
     * - CONVERSATION_TRANSFERRED_OUT to the old agent (conversation removed from their queue)
     * - CONVERSATION_TRANSFERRED_IN to the new agent (conversation added to their queue)
     */
    private void handleConversationTransferred(JsonNode event) {
        String conversationId = event.path("conversationId").asText();
        String fromAgentId = String.valueOf(event.path("fromAgentId").asLong(0));
        String toAgentId = String.valueOf(event.path("toAgentId").asLong(0));
        String reason = event.path("reason").asText("");

        // 1. Notify old agent: conversation removed from their queue
        if (!"0".equals(fromAgentId) && sessionManager.isAgentConnected(fromAgentId)) {
            Map<String, Object> outPayload = buildPayload(event, "CONVERSATION_TRANSFERRED_OUT");
            messagingTemplate.convertAndSendToUser(fromAgentId, "/queue/conversations", outPayload);
            log.info("Pushed CONVERSATION_TRANSFERRED_OUT to agent {} for conversation {}",
                    fromAgentId, conversationId);
        }

        // 2. Notify new agent: conversation added to their queue
        if (!"0".equals(toAgentId) && sessionManager.isAgentConnected(toAgentId)) {
            Map<String, Object> inPayload = buildPayload(event, "CONVERSATION_TRANSFERRED_IN");
            messagingTemplate.convertAndSendToUser(toAgentId, "/queue/conversations", inPayload);
            log.info("Pushed CONVERSATION_TRANSFERRED_IN to agent {} for conversation {}",
                    toAgentId, conversationId);
        }

        log.info("Processed conversation.transferred: conversationId={}, from={}, to={}, reason={}",
                conversationId, fromAgentId, toAgentId, reason);
    }

    /**
     * Build a standardized WebSocket payload from a Kafka event.
     */
    private Map<String, Object> buildPayload(JsonNode event, String wsEventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", wsEventType);
        payload.put("data", objectMapper.convertValue(event, Map.class));
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}
