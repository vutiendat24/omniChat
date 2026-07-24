package com.omnichat.tenant.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantReq {

    @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
    private String tenantName;

    @URL(message = "Logo URL must be valid")
    @Size(max = 500, message = "Logo URL too long")
    private String logoUrl;

    @Size(max = 100, message = "Industry name too long")
    private String industry;

    @Size(max = 255, message = "Contact email too long")
    private String contactEmail;

    @Size(max = 13, message = "Contact phone too long")
    private String contactPhone;

    private String address;

    private Long version; // for optimistic locking
}
