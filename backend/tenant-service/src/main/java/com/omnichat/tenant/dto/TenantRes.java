package com.omnichat.tenant.dto;

import com.omnichat.tenant.domain.entity.TenantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TenantRes {
    private String tenantId;
    private String tenantName;
    private String slug;
    private String logoUrl;
    private String industry;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private TenantStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
