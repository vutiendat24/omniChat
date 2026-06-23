package com.omnichat.conversation.dto;

import lombok.*;

/**
 * Request DTO for manual conversation transfer (UC-303).
 *
 * API: PATCH /api/v1/conversations/{id}/assign
 *
 * Per PRD §3.3 UC-303:
 *   Agent/Supervisor selects a target Agent → optionally enters a transfer reason
 *   → system transfers ownership of the conversation to the new Agent.
 *
 * Per API_Specification_OCM.md §1:
 *   PATCH /api/v1/conversations/{id}/assign → Gán/Điều hướng thủ công hội thoại cho một Agent.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    /**
     * The target agent ID to transfer the conversation to.
     * Required field.
     */
    private Long targetAgentId;

    /**
     * Optional reason for the transfer (per UC-303: "Nhập lý do chuyển (tùy chọn)").
     */
    private String reason;

    /**
     * Validate the request.
     * @return true if targetAgentId is present and valid
     */
    public boolean isValid() {
        return targetAgentId != null && targetAgentId > 0;
    }
}
