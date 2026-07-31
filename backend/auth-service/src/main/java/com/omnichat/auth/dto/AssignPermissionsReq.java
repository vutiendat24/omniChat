package com.omnichat.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionsReq {
    @NotNull(message = "Danh sách quyền không được bỏ trống")
    private List<Long> permissionIds;
}
