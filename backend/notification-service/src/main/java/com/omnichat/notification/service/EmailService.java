package com.omnichat.notification.service;

public interface EmailService {

    /**
     * Gửi email xác thực tài khoản sau khi đăng ký.
     *
     * @param toEmail   địa chỉ email người nhận
     * @param fullName  tên hiển thị của người nhận
     * @param token     verification token (UUID) để tạo link xác thực
     */
    void sendVerificationEmail(String toEmail, String fullName, String token);
}
