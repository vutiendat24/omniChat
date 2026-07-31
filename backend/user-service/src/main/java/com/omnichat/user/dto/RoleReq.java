package com.omnichat.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleReq {
    @NotBlank(message = "Tên Role không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Level không được để trống")
    private Integer level;
}
