package com.omnichat.integration.service.builder;

import com.omnichat.integration.dto.unified.UnifiedMessage;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class ZaloMessageBuilderTest {

    private final ZaloMessageBuilder builder = new ZaloMessageBuilder();

    @Test
    void testSupports() {
        assertTrue(builder.supports("ZALO"));
        assertFalse(builder.supports("FACEBOOK"));
    }

    @Test
    void testBuildPayloadText() {
        UnifiedMessage msg = UnifiedMessage.builder()
                .platform("ZALO")
                .sender(UnifiedMessage.Sender.builder().platformUserId("zalo_1").build())
                .messageType(UnifiedMessage.MessageType.TEXT)
                .content(UnifiedMessage.Content.builder().text("Xin chào!").build())
                .build();

        Map<String, Object> payload = builder.buildPayload(msg);

        assertEquals("zalo_1", ((Map) payload.get("recipient")).get("user_id"));
        assertEquals("Xin chào!", ((Map) payload.get("message")).get("text"));
    }
}
