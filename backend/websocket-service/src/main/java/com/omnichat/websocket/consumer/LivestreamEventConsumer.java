package com.omnichat.websocket.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MOD-REAL-03: Livestream Room Broadcast
 * Consumes livestream comments, batches them by roomId, and pushes to subscribed agents every 500ms.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LivestreamEventConsumer {

    private static final String TOPIC = "omnichat.livestream.events";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Buffer for batching: Map<Destination, List<JsonNode>>
    private final Map<String, List<JsonNode>> commentBuffer = new ConcurrentHashMap<>();
    private final ReentrantLock bufferLock = new ReentrantLock();

    @KafkaListener(topics = TOPIC, groupId = "${spring.application.name}-livestream-group")
    public void consumeLivestreamEvent(Object message, Acknowledgment acknowledgment) {
        try {
            Object eventPayload = unwrapPayload(message);
            JsonNode event = toJsonNode(eventPayload);

            String eventType = event.path("eventType").asText("");

            if ("livestream.comment.new".equals(eventType) || "livestream.comment".equals(eventType)) {
                String tenantId = event.path("tenantId").asText("");
                String roomId = event.path("roomId").asText("");
                
                if (!tenantId.isBlank() && !roomId.isBlank()) {
                    String destination = "/topic/livestream/" + tenantId + "/" + roomId;
                    
                    bufferLock.lock();
                    try {
                        commentBuffer.computeIfAbsent(destination, k -> new ArrayList<>()).add(event);
                    } finally {
                        bufferLock.unlock();
                    }
                }
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process livestream event", e);
            throw new RuntimeException("Failed to process livestream event", e);
        }
    }

    /**
     * Batch push every 500ms to avoid throttling browser with too many WS frames.
     */
    @Scheduled(fixedRate = 500)
    public void flushComments() {
        Map<String, List<JsonNode>> currentBuffer = new HashMap<>();
        
        bufferLock.lock();
        try {
            if (commentBuffer.isEmpty()) return;
            
            // Swap buffer
            for (Map.Entry<String, List<JsonNode>> entry : commentBuffer.entrySet()) {
                currentBuffer.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            commentBuffer.clear();
        } finally {
            bufferLock.unlock();
        }

        for (Map.Entry<String, List<JsonNode>> entry : currentBuffer.entrySet()) {
            String destination = entry.getKey();
            List<JsonNode> comments = entry.getValue();
            
            if (!comments.isEmpty()) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "LIVESTREAM_COMMENTS_BATCH");
                payload.put("data", comments);
                payload.put("timestamp", System.currentTimeMillis());

                messagingTemplate.convertAndSend(destination, payload);
                log.info("Flushed {} comments to destination {}", comments.size(), destination);
            }
        }
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
