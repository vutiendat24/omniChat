package com.omnichat.notification.service.implement;

import com.omnichat.notification.domain.entity.InAppNotification;
import com.omnichat.notification.repository.InAppNotificationRepository;
import com.omnichat.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public void createInAppNotification(Long userId, String eventType, String title, String body) {
        // Step 3: Check preferences (mocking allow all for now)
        
        // Step 4: Save to DB
        InAppNotification notification = InAppNotification.builder()
                .userId(userId)
                .eventType(eventType)
                .title(title)
                .body(body)
                .status("UNREAD")
                .build();
        
        InAppNotification saved = inAppNotificationRepository.save(notification);

        // Step 5: Push event to M10 via Kafka
        Map<String, Object> pushEvent = new HashMap<>();
        pushEvent.put("eventType", "notification.push");
        pushEvent.put("notificationId", saved.getId());
        pushEvent.put("targetUserId", userId);
        pushEvent.put("title", title);
        pushEvent.put("body", body);
        pushEvent.put("originalEventType", eventType);

        kafkaTemplate.send("omnichat.notification.push", String.valueOf(userId), pushEvent);
        log.info("Created and pushed notification for user {}", userId);
    }

    @Override
    public List<InAppNotification> getNotificationsForUser(Long userId) {
        return inAppNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        InAppNotification notification = inAppNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        notification.setStatus("READ");
        notification.setReadAt(LocalDateTime.now());
        inAppNotificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        inAppNotificationRepository.markAllAsReadForUser(userId);
    }
}
