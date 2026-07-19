package com.omnichat.integration.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class ZaloSendApiClient {

    private static final Logger log = LoggerFactory.getLogger(ZaloSendApiClient.class);

    private final RestTemplate restTemplate;
    private final String sendMessageUrl;

    public ZaloSendApiClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${zalo.send-message-url:https://openapi.zalo.me/v3.0/oa/message/cs}") String sendMessageUrl) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(5))
                .setReadTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.sendMessageUrl = sendMessageUrl;
    }

    public boolean sendTextMessage(String recipientUserId, String messageText, String accessToken) {
        Map<String, Object> requestBody = Map.of(
                "recipient", Map.of("user_id", recipientUserId),
                "message", Map.of("text", messageText)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", accessToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    sendMessageUrl, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully sent Zalo message to userId={}", recipientUserId);
                return true;
            }
            log.error("Zalo API returned non-2xx status: {} - {}",
                    response.getStatusCode(), response.getBody());
            return false;
        } catch (HttpClientErrorException e) {
            log.error("Zalo API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("Failed to call Zalo Send API for userId={}", recipientUserId, e);
            return false;
        }
    }
}
