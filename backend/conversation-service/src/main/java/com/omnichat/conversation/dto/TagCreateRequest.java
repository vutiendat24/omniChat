package com.omnichat.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagCreateRequest {
    @NotBlank(message = "Tag name cannot be blank")
    private String name;
    
    private String color;
}
