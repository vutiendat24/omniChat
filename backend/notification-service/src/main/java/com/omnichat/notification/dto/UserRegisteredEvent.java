package com.omnichat.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirror of UserRegisteredEvent published by auth-service.
 * Topic: topic.notification.email
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private String email;
    private String fullName;
    /**
     * null khi đăng ký qua Google SSO (tài khoản đã được kích hoạt ngay).
     * Chỉ gửi email verify khi token != null.
     */
    private String verificationToken;
}
