package com.omnichat.conversation.controller;

import com.omnichat.conversation.dto.QuickReplyDto;
import com.omnichat.conversation.dto.QuickReplyRequest;
import com.omnichat.conversation.service.QuickReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quick-replies")
@RequiredArgsConstructor
public class QuickReplyController {

    private final QuickReplyService quickReplyService;

    @GetMapping
    public ResponseEntity<List<QuickReplyDto>> getQuickReplies(
            @RequestHeader(value = "X-Tenant-Id", required = false, defaultValue = "1") Long tenantId,
            @RequestHeader(value = "X-User-Id") Long agentId) {
        return ResponseEntity.ok(quickReplyService.getAvailableQuickReplies(tenantId, agentId));
    }

    @PostMapping
    public ResponseEntity<?> createQuickReply(
            @RequestHeader(value = "X-Tenant-Id", required = false, defaultValue = "1") Long tenantId,
            @RequestHeader(value = "X-User-Id") Long agentId,
            @RequestHeader(value = "X-User-Role", required = false, defaultValue = "AGENT") String userRole,
            @Valid @RequestBody QuickReplyRequest request) {
        try {
            return ResponseEntity.ok(quickReplyService.createQuickReply(tenantId, agentId, userRole, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuickReply(
            @RequestHeader(value = "X-Tenant-Id", required = false, defaultValue = "1") Long tenantId,
            @RequestHeader(value = "X-User-Id") Long agentId,
            @RequestHeader(value = "X-User-Role", required = false, defaultValue = "AGENT") String userRole,
            @PathVariable Long id,
            @Valid @RequestBody QuickReplyRequest request) {
        try {
            return ResponseEntity.ok(quickReplyService.updateQuickReply(tenantId, agentId, userRole, id, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuickReply(
            @RequestHeader(value = "X-Tenant-Id", required = false, defaultValue = "1") Long tenantId,
            @RequestHeader(value = "X-User-Id") Long agentId,
            @RequestHeader(value = "X-User-Role", required = false, defaultValue = "AGENT") String userRole,
            @PathVariable Long id) {
        try {
            quickReplyService.deleteQuickReply(tenantId, agentId, userRole, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
