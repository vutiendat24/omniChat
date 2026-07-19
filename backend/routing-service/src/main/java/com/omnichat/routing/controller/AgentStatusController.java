package com.omnichat.routing.controller;

import com.omnichat.routing.dto.AgentStatusRequest;
import com.omnichat.routing.dto.AgentStatusResponse;
import com.omnichat.routing.service.AgentStatusService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Task 4.2.1.1 - Agent Status REST Controller
 * API Endpoint: PATCH /api/v1/agents/{id}/status
 *
 * Updates agent online/offline status with DB + Redis sync.
 * Follows the API Specification from docs/API_Specification_OCM.md.
 */
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentStatusController {

    private final AgentStatusService agentStatusService;

    /**
     * PATCH /api/v1/agents/{id}/status
     * Update agent status (ONLINE, BUSY, OFFLINE).
     *
     * Request body:
     * { "status": "ONLINE" }
     *
     * Response: 200 OK with updated agent status details
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AgentStatusResponse> updateAgentStatus(
            @PathVariable Long id,
            @RequestBody AgentStatusRequest request) {

        AgentStatusResponse response = agentStatusService.updateAgentStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/agents/{id}/status
     * Get current agent status (convenience endpoint for debugging/monitoring).
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<AgentStatusResponse> getAgentStatus(@PathVariable Long id) {
        AgentStatusResponse response = agentStatusService.getAgentStatus(id);
        return ResponseEntity.ok(response);
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
                                "field", "status",
                                "issue", ex.getMessage()
                        ))
                )
        ));
    }
}
