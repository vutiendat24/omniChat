package com.omnichat.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.dto.RawWebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class WebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);

    @Value("${oauth2.facebook.client-secret:my_app_secret}")
    private String fbAppSecret;
    
    @Value("${oauth2.zalo.client-secret:my_app_secret}")
    private String zaloAppSecret;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public WebhookSignatureVerifier(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "webhook_events_raw", groupId = "webhook_verifier_group")
    public void verifyAndForward(String eventJson) {
        try {
            RawWebhookEvent event = objectMapper.readValue(eventJson, RawWebhookEvent.class);
            
            if (!StringUtils.hasText(event.getSignature())) {
                log.warn("Security Alert: Missing signature for webhook event from platform: {}. Dropping message.", event.getPlatform());
                return; // Drop
            }
            
            boolean isValid = false;
            if ("FACEBOOK".equalsIgnoreCase(event.getPlatform())) {
                isValid = verifySignature(event.getPayload(), event.getSignature(), fbAppSecret, "sha256=");
            } else if ("ZALO".equalsIgnoreCase(event.getPlatform())) {
                // Zalo often uses just plain hash or X-ZEvent-Signature
                isValid = verifySignature(event.getPayload(), event.getSignature(), zaloAppSecret, ""); 
            } else {
                log.warn("Unknown platform: {}", event.getPlatform());
                return;
            }

            if (isValid) {
                log.debug("Signature verified successfully for {}", event.getPlatform());
                kafkaTemplate.send("webhook_events_verified", eventJson);
            } else {
                log.warn("Security Alert: Invalid signature (Spoofed Webhook). Dropping message from {}", event.getPlatform());
            }

        } catch (Exception e) {
            log.error("Failed to verify webhook signature", e);
        }
    }

    private boolean verifySignature(String payload, String signatureHeader, String appSecret, String prefix) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            String expectedSignature = prefix + hexString.toString();
            return expectedSignature.equals(signatureHeader);
        } catch (Exception e) {
            log.error("Error calculating HMAC", e);
            return false;
        }
    }
}
