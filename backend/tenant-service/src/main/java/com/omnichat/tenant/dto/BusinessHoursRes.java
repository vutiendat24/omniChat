package com.omnichat.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessHoursRes {
    private String tenantId;
    private String timezone;
    private List<DaySchedule> schedule;
}
