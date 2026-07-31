package com.omnichat.auth.service;

import com.omnichat.auth.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_NOTIFICATION_EMAIL = "topic.notification.email";
    private static final String TOPIC_AUTH_LOGOUT = "topic.auth.logout";

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Sending UserRegisteredEvent to Kafka for email: {}", event.getEmail());
        kafkaTemplate.send(TOPIC_NOTIFICATION_EMAIL, event);
    }

    public void sendTokenBlacklistedEvent(com.omnichat.auth.dto.TokenBlacklistedEvent event) {
        log.info("Sending TokenBlacklistedEvent to Kafka for userId: {}", event.getUserId());
        kafkaTemplate.send(TOPIC_AUTH_LOGOUT, event);
    }
}
