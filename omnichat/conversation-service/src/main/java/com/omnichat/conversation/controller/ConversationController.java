package com.omnichat.conversation.controller;

import com.omnichat.conversation.dto.ConversationDto;
import com.omnichat.conversation.dto.MessageDto;
import com.omnichat.conversation.dto.PaginatedResponse;
import com.omnichat.conversation.service.ConversationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * Task 3.3.1.1 - GET /conversations
     * Supports pagination (page, limit), filtering (status), and sorting (sort with - prefix for DESC).
     *
     * Examples:
     *   GET /api/v1/conversations?page=1&limit=20&status=OPEN&sort=-last_activity_at
     *   GET /api/v1/conversations?page=2&limit=10&sort=created_at
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
     *
     * Examples:
     *   GET /api/v1/conversations/abc-123/messages?page=1&limit=50
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<PaginatedResponse<MessageDto>> getMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {

        PaginatedResponse<MessageDto> response = conversationService.getMessages(id, page, limit);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", Map.of(
                        "code", "NOT_FOUND",
                        "message", ex.getMessage()
                )
        ));
    }
}

