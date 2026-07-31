package com.omnichat.integration.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.dto.unified.UnifiedMessage;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ZaloWebhookParser implements WebhookParser {

    private final ObjectMapper objectMapper;

    public ZaloWebhookParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String platform) {
        return "ZALO".equalsIgnoreCase(platform);
    }

    @Override
    public List<UnifiedMessage> parse(String rawPayload) throws Exception {
        JsonNode root = objectMapper.readTree(rawPayload);
        
        UnifiedMessage.UnifiedMessageBuilder builder = UnifiedMessage.builder()
                .platform("ZALO")
                .timestamp(root.has("timestamp") ? root.get("timestamp").asLong() : System.currentTimeMillis());

        if (root.has("sender") && root.get("sender").has("id")) {
            builder.sender(UnifiedMessage.Sender.builder()
                    .platformUserId(root.get("sender").get("id").asText())
                    .build());
        }

        if (root.has("recipient") && root.get("recipient").has("id")) {
            builder.channelId(root.get("recipient").get("id").asText());
        }

        String eventName = root.has("event_name") ? root.get("event_name").asText() : "";

        if ("user_send_text".equals(eventName) && root.has("message")) {
            builder.messageType(UnifiedMessage.MessageType.TEXT);
            builder.content(UnifiedMessage.Content.builder()
                    .text(root.get("message").get("text").asText())
                    .build());
        } else if ("user_send_image".equals(eventName)) {
            builder.messageType(UnifiedMessage.MessageType.IMAGE);
            builder.content(UnifiedMessage.Content.builder().build());
        } else {
            builder.messageType(UnifiedMessage.MessageType.EVENT);
        }

        return Collections.singletonList(builder.build());
    }
}
