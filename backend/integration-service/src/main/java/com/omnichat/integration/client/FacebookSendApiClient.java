package com.omnichat.integration.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;



@Component
public class FacebookSendApiClient {

    private static final Logger log = LoggerFactory.getLogger(FacebookSendApiClient.class);
    private static final String GRAPH_API_URL = "https://graph.facebook.com/v20.0/me/messages";

    private final RestTemplate restTemplate;

    public FacebookSendApiClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(5))
                .setReadTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    /**
     * Send a text message to a Facebook user via the Send API.
     *
     * @param recipientPsid  The Page-Scoped User ID (PSID) of the recipient
     * @param messageText    The text content to send
     * @param pageAccessToken The Page Access Token from channel_connections
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean sendTextMessage(String recipientPsid, String messageText, String pageAccessToken) {
        String url = GRAPH_API_URL + "?access_token=" + pageAccessToken;

        Map<String, Object> requestBody = Map.of(
                "recipient", Map.of("id", recipientPsid),
                "message", Map.of("text", messageText),
                "messaging_type", "RESPONSE"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully sent message to PSID: {}", recipientPsid);
                return true;
            } else {
                log.error("Facebook API returned non-2xx status: {} - {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (HttpClientErrorException e) {
            log.error("Facebook API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("Failed to call Facebook Send API for PSID: {}", recipientPsid, e);
            return false;
        }
    }

    /**
     * Send an attachment (image/file URL) to a Facebook user via the Send API.
     *
     * @param recipientPsid   The PSID of the recipient
     * @param attachmentUrl   The URL of the attachment to send
     * @param attachmentType  The type: "image", "video", "audio", "file"
     * @param pageAccessToken The Page Access Token
     * @return true if successful
     */
    public boolean sendAttachment(String recipientPsid, String attachmentUrl, String attachmentType, String pageAccessToken) {
        String url = GRAPH_API_URL + "?access_token=" + pageAccessToken;

        Map<String, Object> requestBody = Map.of(
                "recipient", Map.of("id", recipientPsid),
                "message", Map.of(
                        "attachment", Map.of(
                                "type", attachmentType,
                                "payload", Map.of("url", attachmentUrl, "is_reusable", true)
                        )
                ),
                "messaging_type", "RESPONSE"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully sent attachment to PSID: {}", recipientPsid);
                return true;
            } else {
                log.error("Facebook API returned non-2xx for attachment: {} - {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send attachment to PSID: {}", recipientPsid, e);
            return false;
        }
    }
}
