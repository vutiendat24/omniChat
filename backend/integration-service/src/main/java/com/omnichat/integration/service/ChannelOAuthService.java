package com.omnichat.integration.service;

import com.omnichat.integration.entity.ChannelConnection;

public interface ChannelOAuthService {
    String getAuthorizationUrl(String tenantId, ChannelConnection.Platform platform);
    void handleCallback(String code, String state, String error);
}
