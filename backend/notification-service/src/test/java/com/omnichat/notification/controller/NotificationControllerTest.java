package com.omnichat.notification.controller;

import com.omnichat.notification.domain.entity.InAppNotification;
import com.omnichat.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    void testGetNotifications() throws Exception {
        InAppNotification notif = new InAppNotification();
        notif.setId(1L);
        notif.setUserId(1L);
        notif.setTitle("Test");
        notif.setStatus("UNREAD");

        when(notificationService.getNotificationsForUser(1L)).thenReturn(List.of(notif));

        mockMvc.perform(get("/api/v1/notifications")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test"));
    }

    @Test
    void testMarkAsRead() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/1/read")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead(1L, 1L);
    }

    @Test
    void testMarkAllAsRead() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/read-all")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());

        verify(notificationService).markAllAsRead(1L);
    }
}
