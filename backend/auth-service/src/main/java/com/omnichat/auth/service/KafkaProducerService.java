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

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Sending UserRegisteredEvent to Kafka for email: {}", event.getEmail());
        kafkaTemplate.send(TOPIC_NOTIFICATION_EMAIL, event);
    }
}
