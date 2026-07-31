package com.omnichat.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationEvent {
    private String recipientEmail;
    private String subject;
    private String templateCode;
    private Map<String, Object> templateData;
}
