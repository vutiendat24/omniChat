package com.omnichat.notification.service.implement;

import com.omnichat.notification.domain.entity.BouncedEmail;
import com.omnichat.notification.domain.entity.EmailDeliveryLog;
import com.omnichat.notification.repository.BouncedEmailRepository;
import com.omnichat.notification.repository.EmailDeliveryLogRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private EmailDeliveryLogRepository deliveryLogRepository;

    @Mock
    private BouncedEmailRepository bouncedEmailRepository;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        // Need to set fromEmail using reflection or via constructor if possible.
        // For testing we will just leave it null (MimeMessageHelper will accept it if we don't mock it strictly)
        // Actually since we inject mocks, fromEmail will be null.
        org.springframework.test.util.ReflectionTestUtils.setField(emailService, "fromEmail", "test@omnichat.com");
    }

    @Test
    void testSendGenericEmail_Success() throws Exception {
        String toEmail = "manager@example.com";
        String subject = "Weekly Report";
        String templateCode = "WEEKLY_REPORT";
        Map<String, Object> templateData = Map.of("totalConversations", 150);

        when(bouncedEmailRepository.existsByEmail(toEmail)).thenReturn(false);
        
        EmailDeliveryLog logRecord = new EmailDeliveryLog();
        logRecord.setId(1L);
        logRecord.setRetryCount(0);
        when(deliveryLogRepository.save(any(EmailDeliveryLog.class))).thenReturn(logRecord);
        
        when(templateEngine.process(eq("email/weekly_report"), any(Context.class))).thenReturn("<html>Report</html>");
        
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendGenericEmail(toEmail, subject, templateCode, templateData);

        verify(mailSender).send(mimeMessage);
        
        ArgumentCaptor<EmailDeliveryLog> logCaptor = ArgumentCaptor.forClass(EmailDeliveryLog.class);
        verify(deliveryLogRepository, times(2)).save(logCaptor.capture());
        
        assertEquals("SENT", logCaptor.getValue().getStatus());
    }

    @Test
    void testSendGenericEmail_Bounced() throws Exception {
        String toEmail = "invalid@example.com";
        String subject = "Weekly Report";
        String templateCode = "WEEKLY_REPORT";

        when(bouncedEmailRepository.existsByEmail(toEmail)).thenReturn(false);
        
        EmailDeliveryLog logRecord = new EmailDeliveryLog();
        logRecord.setId(1L);
        logRecord.setRetryCount(0);
        when(deliveryLogRepository.save(any(EmailDeliveryLog.class))).thenReturn(logRecord);
        
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Report</html>");
        
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        doThrow(new MailSendException("Invalid Addresses")).when(mailSender).send(any(MimeMessage.class));

        emailService.sendGenericEmail(toEmail, subject, templateCode, null);

        // Verify it was marked as BOUNCED and BouncedEmail was saved
        ArgumentCaptor<EmailDeliveryLog> logCaptor = ArgumentCaptor.forClass(EmailDeliveryLog.class);
        verify(deliveryLogRepository, times(3)).save(logCaptor.capture());
        assertEquals("BOUNCED", logCaptor.getValue().getStatus());
        
        verify(bouncedEmailRepository).save(any(BouncedEmail.class));
    }
    
    @Test
    void testSendGenericEmail_RetryThrowException() throws Exception {
        String toEmail = "manager@example.com";
        String subject = "Weekly Report";
        String templateCode = "WEEKLY_REPORT";

        when(bouncedEmailRepository.existsByEmail(toEmail)).thenReturn(false);
        
        EmailDeliveryLog logRecord = new EmailDeliveryLog();
        logRecord.setId(1L);
        logRecord.setRetryCount(0);
        when(deliveryLogRepository.save(any(EmailDeliveryLog.class))).thenReturn(logRecord);
        
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Report</html>");
        
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        doThrow(new RuntimeException("SMTP Timeout")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(RuntimeException.class, () -> {
            emailService.sendGenericEmail(toEmail, subject, templateCode, null);
        });

        ArgumentCaptor<EmailDeliveryLog> logCaptor = ArgumentCaptor.forClass(EmailDeliveryLog.class);
        verify(deliveryLogRepository, times(2)).save(logCaptor.capture());
        assertEquals("FAILED", logCaptor.getValue().getStatus());
        assertEquals(1, logCaptor.getValue().getRetryCount());
    }
}
