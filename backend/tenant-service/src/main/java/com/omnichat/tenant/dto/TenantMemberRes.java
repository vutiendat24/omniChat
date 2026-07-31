package com.omnichat.tenant.dto;

import com.omnichat.tenant.domain.entity.TenantMemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMemberRes {
    private String id;
    private String tenantId;
    private String email;
    private String roleId;
    private TenantMemberStatus status;
    private String message;
    private LocalDateTime invitedAt;
    private LocalDateTime joinedAt;
}
