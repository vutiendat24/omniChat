package com.omnichat.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamRes {
    private String teamId;
    private String teamName;
    private String description;
    private LocalDateTime createdAt;
}
