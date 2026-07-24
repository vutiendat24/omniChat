package com.omnichat.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleReq {
    @NotBlank(message = "Tên vai trò không được bỏ trống")
    private String roleName;
    private String description;
}
