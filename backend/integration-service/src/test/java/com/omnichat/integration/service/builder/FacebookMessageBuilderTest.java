package com.omnichat.integration.service.builder;

import com.omnichat.integration.dto.unified.UnifiedMessage;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class FacebookMessageBuilderTest {

    private final FacebookMessageBuilder builder = new FacebookMessageBuilder();

    @Test
    void testSupports() {
        assertTrue(builder.supports("FACEBOOK"));
        assertFalse(builder.supports("ZALO"));
    }

    @Test
    void testBuildPayloadText() {
        UnifiedMessage msg = UnifiedMessage.builder()
                .platform("FACEBOOK")
                .sender(UnifiedMessage.Sender.builder().platformUserId("user_1").build()) // Actually for outbound, this is the recipient ID
                .messageType(UnifiedMessage.MessageType.TEXT)
                .content(UnifiedMessage.Content.builder().text("Hello!").build())
                .build();

        Map<String, Object> payload = builder.buildPayload(msg);

        assertEquals("user_1", ((Map) payload.get("recipient")).get("id"));
        assertEquals("Hello!", ((Map) payload.get("message")).get("text"));
    }
}
