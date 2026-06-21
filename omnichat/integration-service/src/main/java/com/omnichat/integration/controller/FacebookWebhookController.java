package com.omnichat.integration.controller;

import com.omnichat.integration.service.FacebookWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhook/fb")
@RequiredArgsConstructor
public class FacebookWebhookController {

    private final FacebookWebhookService facebookWebhookService;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        
        try {
            String responseChallenge = facebookWebhookService.verifyWebhook(mode, token, challenge);
            return ResponseEntity.ok(responseChallenge);
        } catch (IllegalArgumentException e) {
            log.error("Webhook verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
        }
    }

    @PostMapping
    public ResponseEntity<String> receiveEvent(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload) {
            
        try {
            facebookWebhookService.processWebhookEvent(payload, signature);
            return ResponseEntity.status(HttpStatus.OK).body("EVENT_RECEIVED");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error processing event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event");
        }
    }
}
