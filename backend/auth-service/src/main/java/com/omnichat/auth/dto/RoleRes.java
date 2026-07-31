package com.omnichat.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleRes {
    private Long id;
    private String name;
    private String description;
    private boolean isSystem;
}
