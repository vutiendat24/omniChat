package com.omnichat.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRes {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
