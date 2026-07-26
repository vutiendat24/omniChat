package com.omnichat.integration.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
public class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Assume WebhookController has a hardcoded or configured verify_token of "my_verify_token"
    }

    @Test
    void givenValidChallenge_whenGetWebhook_thenReturnChallenge() throws Exception {
        mockMvc.perform(get("/webhook/facebook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "my_verify_token") // We'll set this in properties or assume it's injected
                        .param("hub.challenge", "1158201444"))
                .andExpect(status().isOk())
                .andExpect(content().string("1158201444"));
    }

    @Test
    void givenInvalidToken_whenGetWebhook_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/webhook/facebook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong_token")
                        .param("hub.challenge", "1158201444"))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenValidPayload_whenPostWebhook_thenReturn200() throws Exception {
        String payload = "{\"object\":\"page\",\"entry\":[{\"id\":\"123\",\"time\":1234567890,\"messaging\":[]}]}";
        
        when(kafkaTemplate.send(eq("webhook_events_raw"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(post("/webhook/facebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("X-Hub-Signature-256", "sha256=1234567890"))
                .andExpect(status().isOk());

        verify(kafkaTemplate, times(1)).send(eq("webhook_events_raw"), eq(payload));
    }

    @Test
    void givenKafkaError_whenPostWebhook_thenReturn500() throws Exception {
        String payload = "{\"object\":\"page\",\"entry\":[]}";
        
        when(kafkaTemplate.send(eq("webhook_events_raw"), anyString()))
                .thenThrow(new RuntimeException("Kafka is down"));

        mockMvc.perform(post("/webhook/facebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("X-Hub-Signature-256", "sha256=1234567890"))
                .andExpect(status().isInternalServerError());
    }
}
