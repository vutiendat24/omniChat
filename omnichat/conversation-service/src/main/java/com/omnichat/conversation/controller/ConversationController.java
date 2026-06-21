package com.omnichat.conversation.controller;

import com.omnichat.conversation.dto.ConversationDto;
import com.omnichat.conversation.dto.MessageDto;
import com.omnichat.conversation.dto.PaginatedResponse;
import com.omnichat.conversation.dto.SendMessageRequest;
import com.omnichat.conversation.service.ConversationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * Task 3.3.1.1 - GET /conversations
     * Supports pagination (page, limit), filtering (status), and sorting (sort with - prefix for DESC).
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<ConversationDto>> getConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "-last_activity_at") String sort) {

        PaginatedResponse<ConversationDto> response = conversationService.getConversations(page, limit, status, sort);
        return ResponseEntity.ok(response);
    }

    /**
     * Task 3.3.1.2 - GET /conversations/{id}/messages
     * Returns paginated message history for a conversation, sorted by sent_at DESC (newest first).
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<PaginatedResponse<MessageDto>> getMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {

        PaginatedResponse<MessageDto> response = conversationService.getMessages(id, page, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Task 3.3.2.1 - POST /conversations/{id}/messages
     * Agent sends a new message into a conversation.
     * Validation: at least content_text or content_attachments must be present.
     *
     * Note: agentId would normally come from the JWT token (SecurityContext).
     * For now, it is passed as a request header for development convenience.
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageDto> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request,
            @RequestHeader(value = "X-Agent-Id", defaultValue = "0") String agentId) {

        MessageDto messageDto = conversationService.sendAgentMessage(id, request, agentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(messageDto);
    }

    // --- Exception Handlers ---

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", Map.of(
                        "code", "NOT_FOUND",
                        "message", ex.getMessage()
                )
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", Map.of(
                        "code", "VALIDATION_FAILED",
                        "message", ex.getMessage(),
                        "details", List.of(Map.of(
                                "field", "content_text / content_attachments",
                                "issue", ex.getMessage()
                        ))
                )
        ));
    }
}
