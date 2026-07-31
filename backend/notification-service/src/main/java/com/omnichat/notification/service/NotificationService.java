package com.omnichat.notification.service;

import com.omnichat.notification.domain.entity.InAppNotification;
import java.util.List;

public interface NotificationService {

    void createInAppNotification(Long userId, String eventType, String title, String body);

    List<InAppNotification> getNotificationsForUser(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}
