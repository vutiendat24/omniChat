package com.omnichat.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.entity.ChannelConnection;
import com.omnichat.integration.producer.IntegrationEventProducer;
import com.omnichat.integration.repository.ChannelConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZaloWebhookService {

    private final StringRedisTemplate redisTemplate;
    private final IntegrationEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    private final ChannelConnectionRepository channelConnectionRepository;

    public void processWebhookEvent(Long channelConnectionId, String payload) {
        if (channelConnectionId == null || channelConnectionId <= 0) {
            throw new IllegalArgumentException("channelConnectionId is required");
        }
        ChannelConnection connection = channelConnectionRepository
                .findByIdAndStatus(channelConnectionId, ChannelConnection.ConnectionStatus.CONNECTED)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Zalo channel connection not found or disconnected"));
        if (connection.getPlatform() != ChannelConnection.Platform.ZALO) {
            throw new IllegalArgumentException("channelConnectionId does not belong to a Zalo connection");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String eventName = textAt(rootNode, "event_name", "eventName", "type");
            if (!isCustomerMessageEvent(eventName, rootNode)) {
                log.info("Ignoring Zalo webhook event: eventName={}", eventName);
                return;
            }

            String senderId = firstNonBlank(
                    textAt(rootNode.path("sender"), "id", "user_id", "userId"),
                    textAt(rootNode, "sender_id", "senderId", "user_id", "userId"));
            if (senderId == null || senderId.isBlank()) {
                throw new IllegalArgumentException("Missing Zalo sender id");
            }

            JsonNode messageNode = rootNode.path("message");
            String messageId = firstNonBlank(
                    textAt(messageNode, "msg_id", "message_id", "messageId", "id"),
                    textAt(rootNode, "message_id", "messageId"),
                    UUID.randomUUID().toString());
            String messageText = firstNonBlank(
                    textAt(messageNode, "text", "content"),
                    textAt(rootNode, "text", "content"),
                    "");

            String redisKey = "zalo_webhook:msg:" + channelConnectionId + ":" + messageId;
            Boolean isNewMessage = redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, "PROCESSED", Duration.ofDays(1));

            if (Boolean.TRUE.equals(isNewMessage)) {
                log.info("Processing new Zalo message: channelConnectionId={}, messageId={}",
                        channelConnectionId, messageId);
                eventProducer.publishInboundMessageReceived(
                        "ZALO",
                        senderId,
                        channelConnectionId,
                        messageId,
                        messageText,
                        rootNode);
            } else {
                log.warn("Duplicate Zalo message ignored: channelConnectionId={}, messageId={}",
                        channelConnectionId, messageId);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing Zalo webhook payload", e);
            throw new RuntimeException("Error processing Zalo webhook payload", e);
        }
    }

    private boolean isCustomerMessageEvent(String eventName, JsonNode rootNode) {
        if (rootNode.has("message")) {
            return true;
        }
        if (eventName == null || eventName.isBlank()) {
            return false;
        }
        String normalized = eventName.toLowerCase();
        return normalized.contains("user_send")
                || normalized.contains("send_text")
                || normalized.contains("message");
    }

    private String textAt(JsonNode node, String... fieldNames) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull() && !value.asText("").isBlank()) {
                return value.asText();
            }
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
