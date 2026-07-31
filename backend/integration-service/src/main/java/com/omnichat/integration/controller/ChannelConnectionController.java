package com.omnichat.integration.controller;

import com.omnichat.integration.entity.ChannelConnection;
import com.omnichat.integration.service.ChannelOAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/channels/connect")
public class ChannelConnectionController {

    private final ChannelOAuthService channelOAuthService;

    public ChannelConnectionController(ChannelOAuthService channelOAuthService) {
        this.channelOAuthService = channelOAuthService;
    }

    @GetMapping("/url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId,
            @RequestParam("platform") ChannelConnection.Platform platform) {
        
        if (tenantId == null || tenantId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "X-Tenant-ID header is required"));
        }
        
        String url = channelOAuthService.getAuthorizationUrl(tenantId, platform);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription) {
        
        try {
            channelOAuthService.handleCallback(code, state, error);
            return ResponseEntity.ok("Kết nối thành công. Bạn có thể đóng cửa sổ này.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi kết nối: " + e.getMessage());
        }
    }
}
