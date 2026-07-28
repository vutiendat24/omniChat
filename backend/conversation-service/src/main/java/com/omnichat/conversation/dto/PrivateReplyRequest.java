package com.omnichat.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateReplyRequest {
    
    @NotBlank(message = "commentId cannot be blank")
    private String commentId;

    @NotBlank(message = "pageId cannot be blank")
    private String pageId;

    @NotBlank(message = "channelIdentityId cannot be blank")
    private String channelIdentityId;

    @NotBlank(message = "messageText cannot be blank")
    private String messageText;
    
    private java.time.LocalDateTime commentCreatedAt;
}
