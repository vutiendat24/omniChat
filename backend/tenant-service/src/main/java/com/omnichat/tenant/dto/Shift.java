package com.omnichat.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shift {
    
    @NotBlank(message = "Giờ bắt đầu không được để trống")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Giờ bắt đầu phải theo định dạng HH:mm")
    private String startTime;

    @NotBlank(message = "Giờ kết thúc không được để trống")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Giờ kết thúc phải theo định dạng HH:mm")
    private String endTime;
}
