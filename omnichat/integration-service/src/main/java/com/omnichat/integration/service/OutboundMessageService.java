package com.omnichat.integration.service;

import com.omnichat.integration.client.FacebookSendApiClient;
import com.omnichat.integration.client.ZaloSendApiClient;
import com.omnichat.integration.entity.ChannelConnection;
import com.omnichat.integration.producer.IntegrationEventProducer;
import com.omnichat.integration.repository.ChannelConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service responsible for sending agent messages out to external channels (Facebook, Zalo, etc.).
 * Currently implements Facebook Messenger Send API integration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundMessageService {

    private final ChannelConnectionRepository channelConnectionRepository;
    private final FacebookSendApiClient facebookSendApiClient;
    private final ZaloSendApiClient zaloSendApiClient;
    private final IntegrationEventProducer integrationEventProducer;

    /**
     * Process an outbound message event from conversation-service.
     * Looks up the channel connection, calls the external API, and publishes a status event.
     *
     * @param conversationId      The conversation ID
     * @param messageId           The message ID from conversation-service
     * @param recipientExternalId The external user ID (e.g., Facebook PSID)
     * @param channelConnectionId The channel connection ID for access token lookup
     * @param messageText         The message text content
     * @param conversationStatus  The current conversation status
     */
    public void sendMessageToExternalChannel(
            String conversationId,
            String messageId,
            String recipientExternalId,
            Long channelConnectionId,
            String messageText,
            String conversationStatus) {

        log.info("Processing outbound message: conversationId={}, messageId={}, recipient={}",
                conversationId, messageId, recipientExternalId);

        // 1. Look up channel connection for the access token
        ChannelConnection connection = channelConnectionRepository
                .findByIdAndStatus(channelConnectionId, ChannelConnection.ConnectionStatus.CONNECTED)
                .orElse(null);

        if (connection == null) {
            log.error("No active channel connection found for id={}", channelConnectionId);
            publishStatusEvent(conversationId, messageId, "FAILED", "Channel connection not found or disconnected");
            return;
        }

        // 2. Route to the correct platform API
        boolean success;
        switch (connection.getPlatform()) {
            case FACEBOOK:
                success = sendViaFacebook(recipientExternalId, messageText, connection.getAccessToken());
                break;
            case ZALO:
                success = sendViaZalo(recipientExternalId, messageText, connection.getAccessToken());
                break;
            // Future: case SHOPEE, TIKTOK
            default:
                log.warn("Unsupported platform: {}", connection.getPlatform());
                publishStatusEvent(conversationId, messageId, "FAILED", "Unsupported platform: " + connection.getPlatform());
                return;
        }

        // 3. Publish integration.status event
        if (success) {
            publishStatusEvent(conversationId, messageId, "DELIVERED", null);
        } else {
            publishStatusEvent(conversationId, messageId, "FAILED", "External API call failed");
        }
    }

    private boolean sendViaFacebook(String recipientPsid, String messageText, String accessToken) {
        if (messageText == null || messageText.isBlank()) {
            log.warn("Empty message text, skipping Facebook Send API call");
            return true; // Consider attachments-only as handled separately
        }
        return facebookSendApiClient.sendTextMessage(recipientPsid, messageText, accessToken);
    }

    private boolean sendViaZalo(String recipientUserId, String messageText, String accessToken) {
        if (messageText == null || messageText.isBlank()) {
            log.warn("Empty message text, skipping Zalo Send API call");
            return true;
        }
        return zaloSendApiClient.sendTextMessage(recipientUserId, messageText, accessToken);
    }

    /**
     * Publish an integration.status event back to Kafka so conversation-service
     * can update the message delivery status (SENT -> DELIVERED / FAILED).
     */
    private void publishStatusEvent(String conversationId, String messageId, String status, String errorReason) {
        Map<String, Object> statusEvent = Map.of(
                "eventType", "integration.status",
                "conversationId", conversationId,
                "messageId", messageId,
                "deliveryStatus", status,
                "errorReason", errorReason != null ? errorReason : "",
                "timestamp", LocalDateTime.now().toString()
        );

        integrationEventProducer.publishIntegrationMessageReceived(conversationId, statusEvent);
        log.info("Published integration.status event: messageId={}, status={}", messageId, status);
    }
}
