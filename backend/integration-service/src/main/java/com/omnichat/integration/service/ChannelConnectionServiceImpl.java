package com.omnichat.integration.service;

import com.omnichat.integration.entity.ChannelConnection;
import com.omnichat.integration.repository.ChannelConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ChannelConnectionServiceImpl implements ChannelConnectionService {

    private static final Logger log = LoggerFactory.getLogger(ChannelConnectionServiceImpl.class);
    private final ChannelConnectionRepository repository;
    private final RestTemplate restTemplate;

    public ChannelConnectionServiceImpl(ChannelConnectionRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @Override
    public void disconnectChannel(Long id) {
        ChannelConnection channel = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        if (channel.getStatus() != ChannelConnection.ConnectionStatus.CONNECTED &&
            channel.getStatus() != ChannelConnection.ConnectionStatus.ERROR) {
            throw new RuntimeException("Channel is not connected");
        }

        // Optional: Call platform API to revoke token / unsubscribe webhook
        revokePlatformAccess(channel);

        // Update local DB
        channel.setStatus(ChannelConnection.ConnectionStatus.DISCONNECTED);
        channel.setAccessToken(null);
        channel.setRefreshToken(null);
        repository.save(channel);
    }

    private void revokePlatformAccess(ChannelConnection channel) {
        try {
            if (channel.getPlatform() == ChannelConnection.Platform.FACEBOOK) {
                String url = "https://graph.facebook.com/v19.0/" + channel.getChannelId() + "/permissions?access_token=" + channel.getAccessToken();
                restTemplate.delete(url);
            }
            // Zalo / TikTok revoke logic would go here
        } catch (Exception e) {
            log.error("Failed to revoke platform access for channel {}, but will proceed to disconnect locally", channel.getChannelId(), e);
        }
    }
}
