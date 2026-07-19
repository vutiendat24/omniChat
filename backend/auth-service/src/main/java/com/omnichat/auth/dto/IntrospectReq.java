package com.omnichat.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IntrospectReq {
    @NotBlank
    private String token;
}
