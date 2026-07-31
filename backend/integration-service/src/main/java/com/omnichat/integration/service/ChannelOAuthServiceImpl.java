package com.omnichat.integration.service;

import com.omnichat.integration.entity.ChannelConnection;
import com.omnichat.integration.repository.ChannelConnectionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Service
public class ChannelOAuthServiceImpl implements ChannelOAuthService {

    private final ChannelConnectionRepository repository;
    private final RedisStateService redisStateService;
    private final RestTemplate restTemplate;

    @Value("${facebook.app-id:dummy_fb_app_id}")
    private String fbAppId;
    
    @Value("${facebook.app-secret:dummy_fb_app_secret}")
    private String fbAppSecret;

    @Value("${facebook.redirect-uri:http://localhost:8084/api/v1/channels/connect/callback}")
    private String fbRedirectUri;

    public ChannelOAuthServiceImpl(ChannelConnectionRepository repository,
                                   RedisStateService redisStateService,
                                   RestTemplate restTemplate) {
        this.repository = repository;
        this.redisStateService = redisStateService;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getAuthorizationUrl(String tenantId, ChannelConnection.Platform platform) {
        String state = redisStateService.generateState(tenantId, platform);
        if (platform == ChannelConnection.Platform.FACEBOOK) {
            return UriComponentsBuilder.fromHttpUrl("https://www.facebook.com/v19.0/dialog/oauth")
                    .queryParam("client_id", fbAppId)
                    .queryParam("redirect_uri", fbRedirectUri)
                    .queryParam("state", state)
                    .queryParam("scope", "pages_manage_metadata,pages_read_engagement,pages_messaging")
                    .toUriString();
        }
        throw new IllegalArgumentException("Platform not supported yet: " + platform);
    }

    @Override
    public void handleCallback(String code, String state, String error) {
        if (error != null) {
            throw new RuntimeException("User denied access or error occurred: " + error);
        }
        if (state == null || code == null) {
            throw new IllegalArgumentException("State and code are required");
        }

        RedisStateService.OAuthStateData stateData = redisStateService.validateAndGetStateData(state)
                .orElseThrow(() -> new RuntimeException("Invalid or expired state"));

        if (stateData.platform == ChannelConnection.Platform.FACEBOOK) {
            handleFacebookCallback(code, stateData.tenantId);
        } else {
            throw new IllegalArgumentException("Platform not supported yet: " + stateData.platform);
        }
    }

    private void handleFacebookCallback(String code, String tenantId) {
        // 1. Exchange code for user access token
        String tokenUrl = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v19.0/oauth/access_token")
                .queryParam("client_id", fbAppId)
                .queryParam("redirect_uri", fbRedirectUri)
                .queryParam("client_secret", fbAppSecret)
                .queryParam("code", code)
                .toUriString();

        ResponseEntity<Map> tokenResponse = restTemplate.getForEntity(tokenUrl, Map.class);
        if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
            throw new RuntimeException("Failed to get access token from Facebook");
        }
        
        String userAccessToken = (String) tokenResponse.getBody().get("access_token");

        // 2. Get Pages this user manages
        String pagesUrl = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v19.0/me/accounts")
                .queryParam("access_token", userAccessToken)
                .toUriString();
        
        ResponseEntity<Map> pagesResponse = restTemplate.getForEntity(pagesUrl, Map.class);
        if (!pagesResponse.getStatusCode().is2xxSuccessful() || pagesResponse.getBody() == null) {
            throw new RuntimeException("Failed to get pages from Facebook");
        }

        // For simplicity, we assume we connect the first page returned (in reality, frontend sends which page to connect, or we loop through them)
        // Here we just pick the first page to satisfy MOD-CI-01 basic requirement
        java.util.List<Map<String, Object>> data = (java.util.List<Map<String, Object>>) pagesResponse.getBody().get("data");
        if (data == null || data.isEmpty()) {
            throw new RuntimeException("No pages found for this user");
        }
        
        Map<String, Object> firstPage = data.get(0);
        String pageId = (String) firstPage.get("id");
        String pageName = (String) firstPage.get("name");
        String pageAccessToken = (String) firstPage.get("access_token");
        
        // Fetch avatar (Optional, simple mock for now)
        String avatarUrl = "https://graph.facebook.com/" + pageId + "/picture?type=normal";

        saveOrUpdateChannel(tenantId, ChannelConnection.Platform.FACEBOOK, pageId, pageName, pageAccessToken, avatarUrl);
    }

    private void saveOrUpdateChannel(String tenantId, ChannelConnection.Platform platform, String channelId, String pageName, String accessToken, String avatarUrl) {
        // Check uniqueness across the system
        Optional<ChannelConnection> existingGlobal = repository.findByChannelIdAndPlatform(channelId, platform);
        if (existingGlobal.isPresent() && !existingGlobal.get().getTenantId().equals(tenantId)) {
            throw new RuntimeException("Channel already connected to another shop");
        }

        // Check if exists in current tenant
        Optional<ChannelConnection> existingTenant = repository.findByChannelIdAndTenantId(channelId, tenantId);
        ChannelConnection channel;
        
        if (existingTenant.isPresent()) {
            channel = existingTenant.get();
            channel.setAccessToken(accessToken);
            channel.setPageName(pageName);
            channel.setAvatarUrl(avatarUrl);
            channel.setStatus(ChannelConnection.ConnectionStatus.CONNECTED);
        } else {
            channel = ChannelConnection.builder()
                    .platform(platform)
                    .tenantId(tenantId)
                    .channelId(channelId)
                    .pageName(pageName)
                    .accessToken(accessToken)
                    .avatarUrl(avatarUrl)
                    .status(ChannelConnection.ConnectionStatus.CONNECTED)
                    .build();
        }
        
        repository.save(channel);
    }
}
