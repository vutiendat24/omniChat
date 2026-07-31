package com.omnichat.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuickReplyRequest {
    @NotBlank(message = "Shortcut cannot be blank")
    @Size(max = 20, message = "Shortcut max length is 20 characters")
    private String shortcut;

    @NotBlank(message = "Content cannot be blank")
    @Size(max = 2000, message = "Content max length is 2000 characters")
    private String content;

    private Boolean isGlobal = false;
}
