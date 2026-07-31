package com.omnichat.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleReq {
    @NotNull(message = "ID của Role mới không được để trống")
    private Long newRoleId;
}
