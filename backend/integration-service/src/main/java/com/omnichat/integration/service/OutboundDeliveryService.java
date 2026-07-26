package com.omnichat.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.dto.unified.UnifiedMessage;
import com.omnichat.integration.entity.ChannelConnection;
import com.omnichat.integration.repository.ChannelConnectionRepository;
import com.omnichat.integration.service.builder.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OutboundDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(OutboundDeliveryService.class);

    private final List<MessageBuilder> builders;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ChannelConnectionRepository repository;
    private final RestTemplate restTemplate;

    public OutboundDeliveryService(List<MessageBuilder> builders,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper,
                                   ChannelConnectionRepository repository,
                                   RestTemplate restTemplate) {
        this.builders = builders;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "outbound_messages", groupId = "outbound_delivery_group")
    public void deliverMessage(String msgJson) {
        try {
            UnifiedMessage msg = objectMapper.readValue(msgJson, UnifiedMessage.class);

            MessageBuilder builder = builders.stream()
                    .filter(b -> b.supports(msg.getPlatform()))
                    .findFirst()
                    .orElse(null);

            if (builder == null) {
                log.error("No builder found for platform: {}", msg.getPlatform());
                publishDeliveryStatus(msg.getRawPayloadRef(), "FAILED", "Unsupported platform");
                return;
            }

            Optional<ChannelConnection> channelOpt = repository.findByChannelIdAndPlatform(
                    msg.getChannelId(),
                    ChannelConnection.Platform.valueOf(msg.getPlatform().toUpperCase())
            );

            if (channelOpt.isEmpty() || channelOpt.get().getStatus() != ChannelConnection.ConnectionStatus.CONNECTED) {
                log.error("Channel not found or disconnected: {}", msg.getChannelId());
                publishDeliveryStatus(msg.getRawPayloadRef(), "FAILED", "Channel disconnected");
                return;
            }

            String accessToken = channelOpt.get().getAccessToken();
            Map<String, Object> payload = builder.buildPayload(msg);

            String apiUrl = getApiUrl(msg.getPlatform(), accessToken);

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, payload, Map.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Message sent successfully via {}", msg.getPlatform());
                    publishDeliveryStatus(msg.getRawPayloadRef(), "SENT", null);
                }
            } catch (HttpClientErrorException e) {
                log.error("Failed to send message via {}: {}", msg.getPlatform(), e.getResponseBodyAsString());
                
                // Simplified policy check
                String reason = e.getResponseBodyAsString().contains("10") ? "Thất bại (Quá cửa sổ 24h)" : "API Error";
                publishDeliveryStatus(msg.getRawPayloadRef(), "FAILED", reason);
            } catch (Exception e) {
                log.error("Network or unexpected error while sending message", e);
                // In real world, we would push to retry queue here
                publishDeliveryStatus(msg.getRawPayloadRef(), "FAILED", "Network Error");
            }

        } catch (Exception e) {
            log.error("Failed to process outbound message", e);
        }
    }

    private String getApiUrl(String platform, String accessToken) {
        if ("FACEBOOK".equalsIgnoreCase(platform)) {
            return "https://graph.facebook.com/v19.0/me/messages?access_token=" + accessToken;
        } else if ("ZALO".equalsIgnoreCase(platform)) {
            return "https://openapi.zalo.me/v3.0/oa/message/cs?access_token=" + accessToken;
        }
        return "";
    }

    private void publishDeliveryStatus(String messageIdRef, String status, String reason) {
        try {
            Map<String, Object> statusEvent = new HashMap<>();
            statusEvent.put("messageIdRef", messageIdRef);
            statusEvent.put("status", status);
            statusEvent.put("reason", reason);
            statusEvent.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send("message_delivery_status", objectMapper.writeValueAsString(statusEvent));
        } catch (Exception e) {
            log.error("Failed to publish delivery status", e);
        }
    }
}
