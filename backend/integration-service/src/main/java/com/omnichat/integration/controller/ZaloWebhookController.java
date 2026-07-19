package com.omnichat.integration.controller;

import com.omnichat.integration.service.ZaloWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhook/zalo")
@RequiredArgsConstructor
public class ZaloWebhookController {

    private final ZaloWebhookService zaloWebhookService;

    @GetMapping
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("ZALO_WEBHOOK_READY");
    }

    @PostMapping("/{channelConnectionId}")
    public ResponseEntity<String> receiveEventWithPath(
            @PathVariable Long channelConnectionId,
            @RequestBody String payload) {
        return receiveEvent(channelConnectionId, payload);
    }

    @PostMapping
    public ResponseEntity<String> receiveEventWithQuery(
            @RequestParam("channelConnectionId") Long channelConnectionId,
            @RequestBody String payload) {
        return receiveEvent(channelConnectionId, payload);
    }

    private ResponseEntity<String> receiveEvent(Long channelConnectionId, String payload) {
        try {
            zaloWebhookService.processWebhookEvent(channelConnectionId, payload);
            return ResponseEntity.ok("EVENT_RECEIVED");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Zalo webhook request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error processing Zalo event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event");
        }
    }
}
