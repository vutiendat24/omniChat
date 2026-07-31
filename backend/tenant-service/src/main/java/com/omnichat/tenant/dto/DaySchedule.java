package com.omnichat.tenant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaySchedule {

    @NotNull(message = "Ngày trong tuần không được để trống")
    private DayOfWeek dayOfWeek;

    private boolean isDayOff;

    @Valid
    private List<Shift> shifts;
}
