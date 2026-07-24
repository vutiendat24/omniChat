package com.omnichat.tenant.dto;

import com.omnichat.tenant.domain.entity.TenantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TenantRes {
    private String tenantId;
    private TenantStatus status;
    private LocalDateTime createdAt;
}
