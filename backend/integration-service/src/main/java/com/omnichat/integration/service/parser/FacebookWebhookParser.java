package com.omnichat.integration.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.dto.unified.UnifiedMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FacebookWebhookParser implements WebhookParser {

    private final ObjectMapper objectMapper;

    public FacebookWebhookParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String platform) {
        return "FACEBOOK".equalsIgnoreCase(platform);
    }

    @Override
    public List<UnifiedMessage> parse(String rawPayload) throws Exception {
        List<UnifiedMessage> messages = new ArrayList<>();
        JsonNode root = objectMapper.readTree(rawPayload);

        if (root.has("entry") && root.get("entry").isArray()) {
            for (JsonNode entry : root.get("entry")) {
                if (entry.has("messaging") && entry.get("messaging").isArray()) {
                    for (JsonNode messaging : entry.get("messaging")) {
                        messages.add(parseMessagingEvent(messaging));
                    }
                }
            }
        }
        return messages;
    }

    private UnifiedMessage parseMessagingEvent(JsonNode messaging) {
        UnifiedMessage.UnifiedMessageBuilder builder = UnifiedMessage.builder()
                .platform("FACEBOOK")
                .timestamp(messaging.has("timestamp") ? messaging.get("timestamp").asLong() : System.currentTimeMillis());

        if (messaging.has("sender") && messaging.get("sender").has("id")) {
            builder.sender(UnifiedMessage.Sender.builder()
                    .platformUserId(messaging.get("sender").get("id").asText())
                    .build());
        }

        if (messaging.has("recipient") && messaging.get("recipient").has("id")) {
            builder.channelId(messaging.get("recipient").get("id").asText());
        }

        if (messaging.has("message")) {
            JsonNode message = messaging.get("message");
            if (message.has("text")) {
                builder.messageType(UnifiedMessage.MessageType.TEXT);
                builder.content(UnifiedMessage.Content.builder()
                        .text(message.get("text").asText())
                        .build());
            } else if (message.has("attachments")) {
                // Simplified attachment handling
                builder.messageType(UnifiedMessage.MessageType.IMAGE); // Or video/file etc based on type
                builder.content(UnifiedMessage.Content.builder().build());
            } else {
                builder.messageType(UnifiedMessage.MessageType.UNSUPPORTED);
            }
        } else if (messaging.has("read")) {
            builder.messageType(UnifiedMessage.MessageType.READ_RECEIPT);
        } else {
            builder.messageType(UnifiedMessage.MessageType.EVENT);
        }

        return builder.build();
    }
}
