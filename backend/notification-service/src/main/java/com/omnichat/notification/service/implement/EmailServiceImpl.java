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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import com.omnichat.notification.domain.entity.EmailDeliveryLog;
import com.omnichat.notification.domain.entity.BouncedEmail;
import com.omnichat.notification.repository.EmailDeliveryLogRepository;
import com.omnichat.notification.repository.BouncedEmailRepository;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailDeliveryLogRepository deliveryLogRepository;
    private final BouncedEmailRepository bouncedEmailRepository;

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

    @Override
    @Retryable(
        value = {MessagingException.class, Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendGenericEmail(String toEmail, String subject, String templateCode, Map<String, Object> templateData) {
        if (bouncedEmailRepository.existsByEmail(toEmail)) {
            log.warn("Skipping email delivery. Address is bounced: {}", toEmail);
            return;
        }

        EmailDeliveryLog deliveryLog = EmailDeliveryLog.builder()
                .recipientEmail(toEmail)
                .templateCode(templateCode)
                .status("PENDING")
                .build();
        deliveryLog = deliveryLogRepository.save(deliveryLog);

        try {
            Context ctx = new Context();
            if (templateData != null) {
                templateData.forEach(ctx::setVariable);
            }

            // Dùng templateCode làm tên file Thymeleaf. Nếu không có file thật, có thể lỗi templateEngine
            // Để mock up, ta dùng default string nếu file không tồn tại, nhưng thymeleaf yêu cầu file .html
            // Mặc định, templateCode = "report" sẽ trỏ tới templates/email/report.html
            String htmlContent;
            try {
                htmlContent = templateEngine.process("email/" + templateCode.toLowerCase(), ctx);
            } catch (Exception e) {
                // Fallback nếu không có template
                htmlContent = "<p>Message: " + templateData + "</p>";
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            deliveryLog.setStatus("SENT");
            deliveryLogRepository.save(deliveryLog);
            log.info("Email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            deliveryLog.setStatus("FAILED");
            deliveryLog.setErrorMessage(e.getMessage());
            deliveryLog.setRetryCount(deliveryLog.getRetryCount() + 1);
            deliveryLogRepository.save(deliveryLog);

            // Bắt lỗi Bounced (ví dụ: Invalid address)
            if (e.getMessage() != null && e.getMessage().contains("Invalid Addresses")) {
                deliveryLog.setStatus("BOUNCED");
                deliveryLogRepository.save(deliveryLog);
                bouncedEmailRepository.save(BouncedEmail.builder()
                        .email(toEmail)
                        .reason(e.getMessage())
                        .build());
                log.error("Email bounced: {}", toEmail);
                return; // Đừng retry nếu bounce
            }

            log.error("Failed to send email to {}, retry count: {}", toEmail, deliveryLog.getRetryCount(), e);
            throw new RuntimeException("Email delivery failed, triggering retry", e); // Kích hoạt @Retryable
        }
    }
}
