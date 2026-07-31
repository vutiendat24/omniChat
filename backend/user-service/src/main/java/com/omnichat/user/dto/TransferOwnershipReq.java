package com.omnichat.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferOwnershipReq {
    @NotNull(message = "ID của người được trao quyền không được để trống")
    private Long newOwnerUserId;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
