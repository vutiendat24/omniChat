package com.omnichat.websocket.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class LivestreamSubscriptionInterceptorTest {

    private LivestreamSubscriptionInterceptor interceptor;
    private MessageChannel messageChannel;

    @BeforeEach
    void setUp() {
        interceptor = new LivestreamSubscriptionInterceptor();
        messageChannel = mock(MessageChannel.class);
    }

    @Test
    void testPreSend_SubscribeToLivestream_WithValidTenantId_ShouldPass() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/livestream/tenant-abc/room-123");
        
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("tenantId", "tenant-abc");
        accessor.setSessionAttributes(sessionAttributes);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertDoesNotThrow(() -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void testPreSend_SubscribeToLivestream_WithInvalidTenantId_ShouldThrowException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/livestream/tenant-abc/room-123");
        
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("tenantId", "tenant-xyz");
        accessor.setSessionAttributes(sessionAttributes);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void testPreSend_SubscribeToLivestream_WithoutSessionAttributes_ShouldThrowException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/livestream/tenant-abc/room-123");
        
        // No session attributes

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void testPreSend_SubscribeToOtherDestination_ShouldPass() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/conversations"); // not livestream
        
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("tenantId", "tenant-xyz");
        accessor.setSessionAttributes(sessionAttributes);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertDoesNotThrow(() -> interceptor.preSend(message, messageChannel));
    }
}
