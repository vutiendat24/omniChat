package com.omnichat.websocket.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LivestreamEventConsumerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private Acknowledgment acknowledgment;

    private ObjectMapper objectMapper = new ObjectMapper();

    private LivestreamEventConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new LivestreamEventConsumer(messagingTemplate, objectMapper);
    }

    @Test
    void testConsumeLivestreamEvent_ShouldBufferAndFlushBatch() throws Exception {
        // Given
        String payload1 = """
                {
                    "eventType": "livestream.comment.new",
                    "tenantId": "t1",
                    "roomId": "r1",
                    "comment": "hello"
                }
                """;
        
        String payload2 = """
                {
                    "eventType": "livestream.comment.new",
                    "tenantId": "t1",
                    "roomId": "r1",
                    "comment": "world"
                }
                """;

        String payload3 = """
                {
                    "eventType": "livestream.comment.new",
                    "tenantId": "t2",
                    "roomId": "r2",
                    "comment": "hi"
                }
                """;

        // When
        consumer.consumeLivestreamEvent(payload1, acknowledgment);
        consumer.consumeLivestreamEvent(payload2, acknowledgment);
        consumer.consumeLivestreamEvent(payload3, acknowledgment);

        // Then buffer is not flushed yet
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        // When flush is called (simulating @Scheduled)
        consumer.flushComments();

        // Then messages are sent in batches to destinations
        ArgumentCaptor<Map<String, Object>> t1Captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/livestream/t1/r1"), t1Captor.capture());
        
        Map<String, Object> t1Payload = t1Captor.getValue();
        assertEquals("LIVESTREAM_COMMENTS_BATCH", t1Payload.get("type"));
        List<JsonNode> t1Data = (List<JsonNode>) t1Payload.get("data");
        assertEquals(2, t1Data.size());
        assertEquals("hello", t1Data.get(0).path("comment").asText());
        assertEquals("world", t1Data.get(1).path("comment").asText());

        ArgumentCaptor<Map<String, Object>> t2Captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/livestream/t2/r2"), t2Captor.capture());
        
        Map<String, Object> t2Payload = t2Captor.getValue();
        List<JsonNode> t2Data = (List<JsonNode>) t2Payload.get("data");
        assertEquals(1, t2Data.size());
        assertEquals("hi", t2Data.get(0).path("comment").asText());
    }
}
