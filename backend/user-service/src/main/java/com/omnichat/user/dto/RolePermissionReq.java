package com.omnichat.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionReq {
    @NotNull(message = "Danh sách Permission không được để trống")
    private List<Long> permissionIds;
}
