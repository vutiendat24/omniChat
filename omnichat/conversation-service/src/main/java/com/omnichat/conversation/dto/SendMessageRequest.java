package com.omnichat.conversation.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    @Size(max = 2000, message = "content_text must not exceed 2000 characters")
    private String contentText;

    @Size(max = 5, message = "content_attachments must not exceed 5 items")
    private List<String> contentAttachments;

    /**
     * Custom validation: at least one of contentText or contentAttachments must be present.
     * (Matching the API spec's anyOf constraint)
     */
    public boolean isValid() {
        boolean hasText = contentText != null && !contentText.isBlank();
        boolean hasAttachments = contentAttachments != null && !contentAttachments.isEmpty();
        return hasText || hasAttachments;
    }
}
