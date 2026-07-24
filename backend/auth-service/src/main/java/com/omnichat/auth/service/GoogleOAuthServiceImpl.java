package com.omnichat.auth.service;

import com.omnichat.auth.dto.GoogleUserInfo;
import org.springframework.stereotype.Service;

@Service
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    // Normally we'd inject RestTemplate or use GoogleIdTokenVerifier here.
    
    @Override
    public GoogleUserInfo verifyCodeAndGetUserInfo(String code) {
        // Mock implementation for the scope of this task.
        // In reality, this would exchange the code for tokens and call userinfo endpoint.
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Invalid Google authorization code");
        }
        
        return GoogleUserInfo.builder()
                .id("google-12345")
                .email("user@gmail.com")
                .name("Google User")
                .picture("https://google.com/avatar.jpg")
                .build();
    }
}
