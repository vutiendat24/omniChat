package com.omnichat.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.notification.dto.EmailNotificationEvent;
import com.omnichat.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenericEmailEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "omnichat.notification.email", groupId = "${spring.application.name}-generic-email-group")
    public void consume(String message) {
        try {
            EmailNotificationEvent event = objectMapper.readValue(message, EmailNotificationEvent.class);
            log.info("Received generic email event for: {}", event.getRecipientEmail());

            emailService.sendGenericEmail(
                    event.getRecipientEmail(),
                    event.getSubject(),
                    event.getTemplateCode(),
                    event.getTemplateData()
            );
        } catch (Exception e) {
            log.error("Failed to process generic email event: {}", message, e);
        }
    }
}
