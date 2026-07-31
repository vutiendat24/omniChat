package com.omnichat.websocket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.websocket.pubsub.RedisPubSubListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

@Configuration
public class RedisPubSubConfig {

    public static final String SYNC_TOPIC = "omnichat.realtime.sync";

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new ChannelTopic(SYNC_TOPIC));
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisPubSubListener listener, ObjectMapper objectMapper) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(listener, "handleMessage");
        Jackson2JsonRedisSerializer<com.omnichat.websocket.pubsub.SyncMessage> serializer = 
                new Jackson2JsonRedisSerializer<>(objectMapper, com.omnichat.websocket.pubsub.SyncMessage.class);
        adapter.setSerializer(serializer);
        return adapter;
    }
}
