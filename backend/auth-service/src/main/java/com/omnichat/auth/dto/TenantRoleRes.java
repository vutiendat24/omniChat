package com.omnichat.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantRoleRes {
    private Long tenantId;
    private String tenantName;
    private String roleName;
}
