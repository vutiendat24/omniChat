package com.omnichat.conversation.dto;

import com.omnichat.conversation.entity.Conversation.ConversationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStatusRequest {
    @NotNull(message = "Status is required")
    private ConversationStatus status;
    private String reason;
}
