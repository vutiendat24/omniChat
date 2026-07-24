package com.omnichat.tenant.dto;

import com.omnichat.tenant.domain.entity.TenantStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantStatusReq {
    @NotNull(message = "Status cannot be null")
    private TenantStatus status;

    private String reason;
}
