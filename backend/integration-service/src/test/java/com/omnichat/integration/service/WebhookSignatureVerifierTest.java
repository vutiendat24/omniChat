package com.omnichat.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.dto.RawWebhookEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WebhookSignatureVerifierTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookSignatureVerifier verifier;

    private final String appSecret = "my_app_secret";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(verifier, "fbAppSecret", appSecret);
        ReflectionTestUtils.setField(verifier, "zaloAppSecret", appSecret);
    }

    private String generateSignature(String payload, String secret, String prefix) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return prefix + hexString.toString();
    }

    @Test
    void givenValidSignature_whenVerify_thenPushToVerifiedTopic() throws Exception {
        String payload = "{\"test\":\"data\"}";
        String signature = generateSignature(payload, appSecret, "sha256=");
        
        RawWebhookEvent event = new RawWebhookEvent("FACEBOOK", signature, payload);
        String eventJson = "{\"platform\":\"FACEBOOK\",\"signature\":\"" + signature + "\",\"payload\":\"{\\\"test\\\":\\\"data\\\"}\"}";

        when(objectMapper.readValue(eventJson, RawWebhookEvent.class)).thenReturn(event);

        verifier.verifyAndForward(eventJson);

        verify(kafkaTemplate, times(1)).send(eq("webhook_events_verified"), eq(eventJson));
    }

    @Test
    void givenInvalidSignature_whenVerify_thenDropMessage() throws Exception {
        String payload = "{\"test\":\"data\"}";
        String signature = "sha256=invalid_signature";
        
        RawWebhookEvent event = new RawWebhookEvent("FACEBOOK", signature, payload);
        String eventJson = "{\"platform\":\"FACEBOOK\",\"signature\":\"" + signature + "\",\"payload\":\"{\\\"test\\\":\\\"data\\\"}\"}";

        when(objectMapper.readValue(eventJson, RawWebhookEvent.class)).thenReturn(event);

        verifier.verifyAndForward(eventJson);

        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void givenMissingSignature_whenVerify_thenDropMessage() throws Exception {
        String payload = "{\"test\":\"data\"}";
        
        RawWebhookEvent event = new RawWebhookEvent("FACEBOOK", null, payload);
        String eventJson = "{\"platform\":\"FACEBOOK\",\"signature\":null,\"payload\":\"{\\\"test\\\":\\\"data\\\"}\"}";

        when(objectMapper.readValue(eventJson, RawWebhookEvent.class)).thenReturn(event);

        verifier.verifyAndForward(eventJson);

        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }
}
