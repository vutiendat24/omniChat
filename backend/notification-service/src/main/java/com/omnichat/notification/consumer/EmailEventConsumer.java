package com.omnichat.notification.consumer;

import com.omnichat.notification.dto.UserRegisteredEvent;
import com.omnichat.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventConsumer {

    private static final String TOPIC = "topic.notification.email";

    private final EmailService emailService;

    @KafkaListener(topics = TOPIC, groupId = "${spring.application.name}-group")
    public void consume(UserRegisteredEvent event) {
        log.info("Received email notification event for: {}", event.getEmail());

        // verificationToken null → Google SSO, tài khoản đã active ngay, không cần verify
        if (event.getVerificationToken() == null) {
            log.info("Skipping verification email for Google SSO user: {}", event.getEmail());
            return;
        }

        emailService.sendVerificationEmail(
                event.getEmail(),
                event.getFullName(),
                event.getVerificationToken()
        );
    }
}
