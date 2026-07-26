package com.omnichat.integration.service.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.integration.dto.unified.UnifiedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FacebookWebhookParserTest {

    private FacebookWebhookParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new FacebookWebhookParser(objectMapper);
    }

    @Test
    void testSupports() {
        assertTrue(parser.supports("FACEBOOK"));
        assertFalse(parser.supports("ZALO"));
    }

    @Test
    void testParseBatchingMessages() throws Exception {
        String payload = "{" +
                "\"object\": \"page\"," +
                "\"entry\": [" +
                "  {" +
                "    \"id\": \"page_123\"," +
                "    \"time\": 1458692752478," +
                "    \"messaging\": [" +
                "      {" +
                "        \"sender\": {\"id\": \"user_1\"}," +
                "        \"recipient\": {\"id\": \"page_123\"}," +
                "        \"timestamp\": 1458692752478," +
                "        \"message\": {" +
                "          \"text\": \"Hello World\"" +
                "        }" +
                "      }," +
                "      {" +
                "        \"sender\": {\"id\": \"user_2\"}," +
                "        \"recipient\": {\"id\": \"page_123\"}," +
                "        \"timestamp\": 1458692752480," +
                "        \"message\": {" +
                "          \"text\": \"Hi\"" +
                "        }" +
                "      }" +
                "    ]" +
                "  }" +
                "]" +
                "}";

        List<UnifiedMessage> messages = parser.parse(payload);
        assertEquals(2, messages.size());

        UnifiedMessage m1 = messages.get(0);
        assertEquals("FACEBOOK", m1.getPlatform());
        assertEquals("page_123", m1.getChannelId());
        assertEquals("user_1", m1.getSender().getPlatformUserId());
        assertEquals(UnifiedMessage.MessageType.TEXT, m1.getMessageType());
        assertEquals("Hello World", m1.getContent().getText());
        assertEquals(1458692752478L, m1.getTimestamp());

        UnifiedMessage m2 = messages.get(1);
        assertEquals("user_2", m2.getSender().getPlatformUserId());
        assertEquals("Hi", m2.getContent().getText());
    }

    @Test
    void testParseReadReceipt() throws Exception {
        String payload = "{" +
                "\"object\": \"page\"," +
                "\"entry\": [" +
                "  {" +
                "    \"id\": \"page_123\"," +
                "    \"time\": 1458692752478," +
                "    \"messaging\": [" +
                "      {" +
                "        \"sender\": {\"id\": \"user_1\"}," +
                "        \"recipient\": {\"id\": \"page_123\"}," +
                "        \"timestamp\": 1458692752478," +
                "        \"read\": {" +
                "          \"watermark\": 1458692752478," +
                "          \"seq\": 38" +
                "        }" +
                "      }" +
                "    ]" +
                "  }" +
                "]" +
                "}";

        List<UnifiedMessage> messages = parser.parse(payload);
        assertEquals(1, messages.size());

        UnifiedMessage m = messages.get(0);
        assertEquals(UnifiedMessage.MessageType.READ_RECEIPT, m.getMessageType());
        assertEquals("page_123", m.getChannelId());
        assertEquals("user_1", m.getSender().getPlatformUserId());
    }
}
