package com.omnichat.notification.service.implement;

import com.omnichat.notification.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        try {
            String verifyLink = frontendUrl + "/verify-email?token=" + token;

            // Build Thymeleaf context
            Context ctx = new Context();
            ctx.setVariable("fullName", fullName);
            ctx.setVariable("verifyLink", verifyLink);

            String htmlContent = templateEngine.process("email/verify-email", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("OmniChat – Xác thực tài khoản của bạn");
            helper.setText(htmlContent, true); // true = send as HTML

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            // Log lỗi nhưng không re-throw để Kafka không retry vô tận
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {}: {}", toEmail, e.getMessage());
        }
    }
}
