package com.omnichat.dummy.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dummy")
@RefreshScope
public class DummyController {

    @Value("${dummy.message:Default Message}")
    private String dummyMessage;

    @GetMapping("/ping")
    public Map<String, String> ping(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "pong");
        response.put("message", dummyMessage); // This will refresh dynamically if config changes
        
        if (userId != null) {
            response.put("userId", userId);
            response.put("roles", roles);
            response.put("authenticated", "true");
        } else {
            response.put("authenticated", "false");
        }

        return response;
    }
}
