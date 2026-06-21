package com.omnichat.conversation.controller;

import com.omnichat.conversation.dto.ConversationDto;
import com.omnichat.conversation.dto.PaginatedResponse;
import com.omnichat.conversation.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
