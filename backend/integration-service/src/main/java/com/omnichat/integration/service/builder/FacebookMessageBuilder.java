package com.omnichat.integration.service.builder;

import com.omnichat.integration.dto.unified.UnifiedMessage;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class FacebookMessageBuilder implements MessageBuilder {

    @Override
    public boolean supports(String platform) {
        return "FACEBOOK".equalsIgnoreCase(platform);
    }

    @Override
    public Map<String, Object> buildPayload(UnifiedMessage msg) {
        Map<String, Object> payload = new HashMap<>();
        
        // Facebook API format requires 'recipient.id'
        Map<String, String> recipient = new HashMap<>();
        // In outbound message, the sender object actually holds the platform user's ID
        recipient.put("id", msg.getSender().getPlatformUserId());
        payload.put("recipient", recipient);

        Map<String, Object> message = new HashMap<>();
        if (msg.getMessageType() == UnifiedMessage.MessageType.TEXT) {
            message.put("text", msg.getContent().getText());
        } else {
            // Simplified handling for images/other types
            message.put("text", "[Attachment]");
        }
        
        payload.put("message", message);
        payload.put("messaging_type", "RESPONSE"); // standard for 24h window
        return payload;
    }
}
