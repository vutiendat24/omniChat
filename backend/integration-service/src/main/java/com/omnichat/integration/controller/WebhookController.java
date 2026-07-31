package com.omnichat.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.dto.RawWebhookEvent;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/raw")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Value("${oauth2.facebook.verify-token:my_verify_token}")
    private String verifyToken;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public WebhookController(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/facebook")
    public ResponseEntity<String> verifyFacebookWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {
        
        log.info("Received Facebook Webhook verification request");
        
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("Webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }
        
        log.warn("Webhook verification failed. Expected token: {}, Received: {}", verifyToken, token);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/facebook")
    public ResponseEntity<String> receiveFacebookWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload) {
        log.debug("Received Facebook Webhook event");
        try {
            RawWebhookEvent event = RawWebhookEvent.builder()
                    .platform("FACEBOOK")
                    .signature(signature)
                    .payload(payload)
                    .build();
            kafkaTemplate.send("webhook_events_raw", objectMapper.writeValueAsString(event));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to push webhook payload to Kafka", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Similarly for Zalo
    @PostMapping("/zalo")
    public ResponseEntity<String> receiveZaloWebhook(
            @RequestHeader(value = "X-ZEvent-Signature", required = false) String signature,
            @RequestBody String payload) {
        log.debug("Received Zalo Webhook event");
        try {
            RawWebhookEvent event = RawWebhookEvent.builder()
                    .platform("ZALO")
                    .signature(signature)
                    .payload(payload)
                    .build();
            kafkaTemplate.send("webhook_events_raw", objectMapper.writeValueAsString(event));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to push webhook payload to Kafka", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
