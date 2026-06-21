package com.omnichat.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.producer.IntegrationEventProducer;
import com.omnichat.integration.util.HmacUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacebookWebhookService {

    private final HmacUtil hmacUtil;
    private final StringRedisTemplate redisTemplate;
    private final IntegrationEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Value("${facebook.app-secret:default-secret}")
    private String appSecret;

    @Value("${facebook.verify-token:default-token}")
    private String verifyToken;

    public String verifyWebhook(String mode, String token, String challenge) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WEBHOOK_VERIFIED");
            return challenge;
        }
        throw new IllegalArgumentException("Invalid verification request");
    }

    public void processWebhookEvent(String payload, String signature) {
        if (!hmacUtil.verifySignature(payload, signature, appSecret)) {
            log.error("Invalid HMAC signature");
            throw new SecurityException("Invalid HMAC signature");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            
            // Expected facebook webhook structure: root -> entry -> messaging -> message.mid
            if (rootNode.has("entry")) {
                JsonNode entryNode = rootNode.path("entry").get(0);
                if (entryNode.has("messaging")) {
                    JsonNode messagingNode = entryNode.path("messaging").get(0);
                    
                    if (messagingNode != null && messagingNode.has("message")) {
                        String messageId = messagingNode.path("message").path("mid").asText();
                        String senderId = messagingNode.path("sender").path("id").asText();
                        
                        // Idempotency Check using Redis
                        String redisKey = "fb_webhook:msg:" + messageId;
                        Boolean isNewMessage = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSED", Duration.ofDays(1));
                        
                        if (Boolean.TRUE.equals(isNewMessage)) {
                            log.info("Processing new message ID: {}", messageId);
                            // Publish to Kafka
                            eventProducer.publishIntegrationMessageReceived(senderId, rootNode);
                        } else {
                            log.warn("Duplicate message ID ignored: {}", messageId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing webhook payload", e);
            throw new RuntimeException("Error processing webhook payload", e);
        }
    }
}
