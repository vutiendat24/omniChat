package com.omnichat.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTeamReq {

    @NotBlank(message = "Team name is required")
    @Size(min = 3, max = 50, message = "Team name must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 _-]+$", message = "Team name contains invalid characters")
    private String teamName;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    @NotNull(message = "Version is required for optimistic locking")
    private Long version;
}
