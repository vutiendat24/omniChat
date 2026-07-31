package com.omnichat.tenant;

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
import com.omnichat.tenant.service.BusinessHoursService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BusinessHoursServiceTest {

    @Mock
    private BusinessHoursRepository businessHoursRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BusinessHoursService businessHoursService;

    private String tenantId;

    @BeforeEach
    void setUp() {
        tenantId = "tenant-1";
    }

    @Test
    void testGetBusinessHours_Default() {
        when(businessHoursRepository.findById(tenantId)).thenReturn(Optional.empty());

        BusinessHoursRes res = businessHoursService.getBusinessHours(tenantId);

        assertNotNull(res);
        assertEquals("UTC", res.getTimezone());
        assertEquals(7, res.getSchedule().size());
        assertFalse(res.getSchedule().get(0).isDayOff());
        assertEquals(1, res.getSchedule().get(0).getShifts().size());
        assertEquals("00:00", res.getSchedule().get(0).getShifts().get(0).getStartTime());
        assertEquals("23:59", res.getSchedule().get(0).getShifts().get(0).getEndTime());
    }

    @Test
    void testGetBusinessHours_Existing() throws JsonProcessingException {
        BusinessHours bh = BusinessHours.builder()
                .tenantId(tenantId)
                .timezone("Asia/Ho_Chi_Minh")
                .scheduleJson("[]")
                .build();
        when(businessHoursRepository.findById(tenantId)).thenReturn(Optional.of(bh));
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(new ArrayList<>());

        BusinessHoursRes res = businessHoursService.getBusinessHours(tenantId);

        assertEquals("Asia/Ho_Chi_Minh", res.getTimezone());
        assertTrue(res.getSchedule().isEmpty());
    }

    @Test
    void testUpdateBusinessHours_Success() throws JsonProcessingException {
        List<DaySchedule> schedule = createValidSchedule();
        BusinessHoursReq req = new BusinessHoursReq("Asia/Ho_Chi_Minh", schedule);

        when(businessHoursRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        BusinessHoursRes res = businessHoursService.updateBusinessHours(tenantId, req);

        assertEquals("Asia/Ho_Chi_Minh", res.getTimezone());
        verify(businessHoursRepository, times(1)).save(any(BusinessHours.class));
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void testUpdateBusinessHours_InvalidTimezone_ThrowsException() {
        BusinessHoursReq req = new BusinessHoursReq("Invalid/Timezone", createValidSchedule());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> businessHoursService.updateBusinessHours(tenantId, req));
        assertTrue(ex.getMessage().contains("Múi giờ không hợp lệ"));
    }

    @Test
    void testUpdateBusinessHours_MissingDays_ThrowsException() {
        List<DaySchedule> schedule = createValidSchedule();
        schedule.remove(0); // Remove Monday
        BusinessHoursReq req = new BusinessHoursReq("UTC", schedule);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> businessHoursService.updateBusinessHours(tenantId, req));
        assertEquals("Lịch làm việc phải cấu hình đầy đủ 7 ngày", ex.getMessage());
    }

    @Test
    void testUpdateBusinessHours_DuplicateDays_ThrowsException() {
        List<DaySchedule> schedule = createValidSchedule();
        schedule.add(schedule.get(0)); // Add Monday again
        BusinessHoursReq req = new BusinessHoursReq("UTC", schedule);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> businessHoursService.updateBusinessHours(tenantId, req));
        assertTrue(ex.getMessage().contains("Ngày trong tuần bị trùng lặp"));
    }

    @Test
    void testUpdateBusinessHours_EmptyShiftsOnWorkingDay_ThrowsException() {
        List<DaySchedule> schedule = createValidSchedule();
        schedule.get(0).setShifts(new ArrayList<>()); // Empty shifts for Monday
        BusinessHoursReq req = new BusinessHoursReq("UTC", schedule);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> businessHoursService.updateBusinessHours(tenantId, req));
        assertTrue(ex.getMessage().contains("Ngày làm việc phải có ít nhất một ca làm việc"));
    }

    @Test
    void testUpdateBusinessHours_EndBeforeStart_ThrowsException() {
        List<DaySchedule> schedule = createValidSchedule();
        schedule.get(0).setShifts(List.of(new Shift("17:00", "08:00")));
        BusinessHoursReq req = new BusinessHoursReq("UTC", schedule);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> businessHoursService.updateBusinessHours(tenantId, req));
        assertTrue(ex.getMessage().contains("Giờ kết thúc phải sau giờ bắt đầu"));
    }

    @Test
    void testUpdateBusinessHours_OverlapShifts_ThrowsException() {
        List<DaySchedule> schedule = createValidSchedule();
        schedule.get(0).setShifts(List.of(
                new Shift("08:00", "12:00"),
                new Shift("11:00", "15:00")
        ));
        BusinessHoursReq req = new BusinessHoursReq("UTC", schedule);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> businessHoursService.updateBusinessHours(tenantId, req));
        assertTrue(ex.getMessage().contains("Ca làm việc bị trùng lặp"));
    }

    private List<DaySchedule> createValidSchedule() {
        List<DaySchedule> schedule = new ArrayList<>();
        List<Shift> shifts = List.of(new Shift("08:00", "17:00"));
        for (DayOfWeek day : DayOfWeek.values()) {
            schedule.add(DaySchedule.builder()
                    .dayOfWeek(day)
                    .isDayOff(false)
                    .shifts(shifts)
                    .build());
        }
        return schedule;
    }
}
