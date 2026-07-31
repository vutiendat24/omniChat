package com.omnichat.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleSsoReq {
    @NotBlank
    private String code;
}
