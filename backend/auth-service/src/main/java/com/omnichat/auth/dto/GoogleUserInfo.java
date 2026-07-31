package com.omnichat.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleUserInfo {
    private String id;
    private String email;
    private String name;
    private String picture;
}
