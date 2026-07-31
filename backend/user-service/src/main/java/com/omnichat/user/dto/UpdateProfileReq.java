package com.omnichat.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileReq {
    @NotBlank(message = "Tên không được để trống")
    @Size(min = 2, max = 100, message = "Tên phải từ 2 đến 100 ký tự")
    @Pattern(regexp = "^[^<>]*$", message = "Tên không được chứa ký tự đặc biệt nguy hiểm")
    private String fullName;

    @URL(message = "Avatar URL không hợp lệ")
    private String avatarUrl;
}
