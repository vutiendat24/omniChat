package com.omnichat.integration.service.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.dto.unified.UnifiedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ZaloWebhookParserTest {

    private ZaloWebhookParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new ZaloWebhookParser(objectMapper);
    }

    @Test
    void testSupports() {
        assertTrue(parser.supports("ZALO"));
        assertFalse(parser.supports("FACEBOOK"));
    }

    @Test
    void testParseText() throws Exception {
        String payload = "{" +
                "\"event_name\": \"user_send_text\"," +
                "\"sender\": {\"id\": \"user_zalo_1\"}," +
                "\"recipient\": {\"id\": \"oa_123\"}," +
                "\"message\": {" +
                "  \"text\": \"Xin chào\"," +
                "  \"msg_id\": \"msg_1\"" +
                "}," +
                "\"timestamp\": 1458692752478" +
                "}";

        List<UnifiedMessage> messages = parser.parse(payload);
        assertEquals(1, messages.size());

        UnifiedMessage m = messages.get(0);
        assertEquals("ZALO", m.getPlatform());
        assertEquals("oa_123", m.getChannelId());
        assertEquals("user_zalo_1", m.getSender().getPlatformUserId());
        assertEquals(UnifiedMessage.MessageType.TEXT, m.getMessageType());
        assertEquals("Xin chào", m.getContent().getText());
        assertEquals(1458692752478L, m.getTimestamp());
    }
}
