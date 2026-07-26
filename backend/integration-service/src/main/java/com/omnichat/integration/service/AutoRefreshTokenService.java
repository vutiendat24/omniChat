package com.omnichat.integration.service;

import com.omnichat.integration.entity.ChannelConnection;
import com.omnichat.integration.repository.ChannelConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AutoRefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(AutoRefreshTokenService.class);

    private final ChannelConnectionRepository repository;
    private final RestTemplate restTemplate;

    @Value("${oauth2.facebook.client-id:}")
    private String fbClientId;

    @Value("${oauth2.facebook.client-secret:}")
    private String fbClientSecret;

    public AutoRefreshTokenService(ChannelConnectionRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    /**
     * Chạy định kỳ (VD: mỗi giờ) để làm mới token sắp hết hạn
     */
    @Scheduled(cron = "0 0 * * * *")
    public void refreshTokens() {
        log.info("Bắt đầu job làm mới token tự động...");
        
        // Ngưỡng làm mới: 24h
        LocalDateTime threshold = LocalDateTime.now().plusHours(24);
        List<ChannelConnection> expiringChannels = repository.findByStatusAndExpiryDateBefore(ChannelConnection.ConnectionStatus.CONNECTED, threshold);

        log.info("Tìm thấy {} kênh cần làm mới token", expiringChannels.size());

        for (ChannelConnection channel : expiringChannels) {
            try {
                refreshChannelToken(channel);
                repository.save(channel);
            } catch (Exception e) {
                log.error("Lỗi khi làm mới token cho kênh {}: {}", channel.getChannelId(), e.getMessage());
                // Nếu là lỗi Client (4xx) thì đánh dấu lỗi kênh
                if (e instanceof HttpClientErrorException) {
                    channel.setStatus(ChannelConnection.ConnectionStatus.ERROR);
                    repository.save(channel);
                }
            }
        }
        
        log.info("Hoàn tất job làm mới token tự động.");
    }

    private void refreshChannelToken(ChannelConnection channel) {
        if (channel.getPlatform() == ChannelConnection.Platform.FACEBOOK) {
            String url = String.format("https://graph.facebook.com/v19.0/oauth/access_token?grant_type=fb_exchange_token&client_id=%s&client_secret=%s&fb_exchange_token=%s",
                    fbClientId, fbClientSecret, channel.getAccessToken());
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (body.containsKey("access_token")) {
                    channel.setAccessToken((String) body.get("access_token"));
                }
                if (body.containsKey("expires_in")) {
                    int expiresIn = (Integer) body.get("expires_in");
                    channel.setExpiryDate(LocalDateTime.now().plusSeconds(expiresIn));
                }
            }
        }
        // Thêm Zalo, TikTok,...
    }
}
