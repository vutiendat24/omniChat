package com.omnichat.tenant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessHoursReq {

    @NotBlank(message = "Timezone không được để trống")
    private String timezone;

    @NotNull(message = "Lịch làm việc không được để trống")
    @Size(min = 7, max = 7, message = "Lịch làm việc phải có đủ 7 ngày")
    @Valid
    private List<DaySchedule> schedule;
}
