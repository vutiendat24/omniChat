package com.omnichat.integration.service.builder;

import com.omnichat.integration.dto.unified.UnifiedMessage;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class ZaloMessageBuilder implements MessageBuilder {

    @Override
    public boolean supports(String platform) {
        return "ZALO".equalsIgnoreCase(platform);
    }

    @Override
    public Map<String, Object> buildPayload(UnifiedMessage msg) {
        Map<String, Object> payload = new HashMap<>();
        
        Map<String, String> recipient = new HashMap<>();
        recipient.put("user_id", msg.getSender().getPlatformUserId());
        payload.put("recipient", recipient);

        Map<String, Object> message = new HashMap<>();
        if (msg.getMessageType() == UnifiedMessage.MessageType.TEXT) {
            message.put("text", msg.getContent().getText());
        } else {
            message.put("text", "[Attachment]");
        }
        
        payload.put("message", message);
        return payload;
    }
}
