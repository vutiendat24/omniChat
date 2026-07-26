package com.omnichat.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Value("${oauth2.facebook.verify-token:my_verify_token}")
    private String verifyToken;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public WebhookController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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
    public ResponseEntity<String> receiveFacebookWebhook(@RequestBody String payload) {
        log.debug("Received Facebook Webhook event");
        try {
            kafkaTemplate.send("webhook_events_raw", payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to push webhook payload to Kafka", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Similarly for Zalo
    @PostMapping("/zalo")
    public ResponseEntity<String> receiveZaloWebhook(@RequestBody String payload) {
        log.debug("Received Zalo Webhook event");
        try {
            kafkaTemplate.send("webhook_events_raw", payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to push webhook payload to Kafka", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
