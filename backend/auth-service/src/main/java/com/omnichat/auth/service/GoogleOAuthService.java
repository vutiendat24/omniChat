package com.omnichat.auth.service;

import com.omnichat.auth.dto.GoogleUserInfo;

public interface GoogleOAuthService {
    GoogleUserInfo verifyCodeAndGetUserInfo(String code);
}
