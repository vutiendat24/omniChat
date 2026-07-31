package com.omnichat.tenant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.tenant.domain.entity.BusinessHours;
import com.omnichat.tenant.domain.entity.OutboxEvent;
import com.omnichat.tenant.dto.BusinessHoursReq;
import com.omnichat.tenant.dto.BusinessHoursRes;
import com.omnichat.tenant.dto.DaySchedule;
import com.omnichat.tenant.dto.Shift;
import com.omnichat.tenant.repository.BusinessHoursRepository;
import com.omnichat.tenant.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessHoursService {

    private final BusinessHoursRepository businessHoursRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public BusinessHoursRes getBusinessHours(String tenantId) {
        Optional<BusinessHours> businessHoursOpt = businessHoursRepository.findById(tenantId);
        if (businessHoursOpt.isEmpty()) {
            return getDefaultBusinessHours(tenantId);
        }

        BusinessHours businessHours = businessHoursOpt.get();
        List<DaySchedule> schedule = parseSchedule(businessHours.getScheduleJson());
        return BusinessHoursRes.builder()
                .tenantId(tenantId)
                .timezone(businessHours.getTimezone())
                .schedule(schedule)
                .build();
    }

    @Transactional
    public BusinessHoursRes updateBusinessHours(String tenantId, BusinessHoursReq request) {
        // Validate timezone
        try {
            ZoneId.of(request.getTimezone());
        } catch (Exception e) {
            throw new IllegalArgumentException("Múi giờ không hợp lệ: " + request.getTimezone());
        }

        // Validate schedule logic
        validateSchedule(request.getSchedule());

        String scheduleJson;
        try {
            scheduleJson = objectMapper.writeValueAsString(request.getSchedule());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize schedule", e);
        }

        BusinessHours businessHours = businessHoursRepository.findById(tenantId)
                .orElse(BusinessHours.builder().tenantId(tenantId).build());

        businessHours.setTimezone(request.getTimezone());
        businessHours.setScheduleJson(scheduleJson);

        businessHoursRepository.save(businessHours);

        publishBusinessHoursUpdatedEvent(tenantId, request.getTimezone(), request.getSchedule());

        return BusinessHoursRes.builder()
                .tenantId(tenantId)
                .timezone(request.getTimezone())
                .schedule(request.getSchedule())
                .build();
    }

    private void validateSchedule(List<DaySchedule> schedule) {
        Set<DayOfWeek> daysSeen = new HashSet<>();
        for (DaySchedule day : schedule) {
            if (!daysSeen.add(day.getDayOfWeek())) {
                throw new IllegalArgumentException("Ngày trong tuần bị trùng lặp: " + day.getDayOfWeek());
            }

            if (day.isDayOff()) {
                continue; // No shifts needed if it's a day off
            }

            if (day.getShifts() == null || day.getShifts().isEmpty()) {
                throw new IllegalArgumentException("Ngày làm việc phải có ít nhất một ca làm việc: " + day.getDayOfWeek());
            }

            List<Shift> sortedShifts = new ArrayList<>(day.getShifts());
            // Ensure no invalid shifts or overlaps
            LocalTime previousEndTime = null;
            sortedShifts.sort(Comparator.comparing(Shift::getStartTime));
            
            for (Shift shift : sortedShifts) {
                LocalTime start = LocalTime.parse(shift.getStartTime());
                LocalTime end = LocalTime.parse(shift.getEndTime());

                if (!end.isAfter(start)) {
                    throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu trong ngày " + day.getDayOfWeek());
                }

                if (previousEndTime != null && start.isBefore(previousEndTime)) {
                    throw new IllegalArgumentException("Ca làm việc bị trùng lặp trong ngày " + day.getDayOfWeek());
                }
                previousEndTime = end;
            }
        }
        
        if (daysSeen.size() != 7) {
            throw new IllegalArgumentException("Lịch làm việc phải cấu hình đầy đủ 7 ngày");
        }
    }

    private void publishBusinessHoursUpdatedEvent(String tenantId, String timezone, List<DaySchedule> schedule) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("timezone", timezone);
        payload.put("schedule", schedule);

        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("BusinessHours")
                    .aggregateId(tenantId)
                    .type("tenant.business_hours_updated")
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }

    private BusinessHoursRes getDefaultBusinessHours(String tenantId) {
        List<DaySchedule> schedule = new ArrayList<>();
        List<Shift> defaultShifts = List.of(new Shift("00:00", "23:59"));
        for (DayOfWeek day : DayOfWeek.values()) {
            schedule.add(DaySchedule.builder()
                    .dayOfWeek(day)
                    .isDayOff(false)
                    .shifts(defaultShifts)
                    .build());
        }
        return BusinessHoursRes.builder()
                .tenantId(tenantId)
                .timezone("UTC")
                .schedule(schedule)
                .build();
    }

    private List<DaySchedule> parseSchedule(String scheduleJson) {
        try {
            return objectMapper.readValue(scheduleJson, new TypeReference<List<DaySchedule>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse schedule JSON", e);
        }
    }
}
