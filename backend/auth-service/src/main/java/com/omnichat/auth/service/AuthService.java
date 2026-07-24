package com.omnichat.auth.service;

import com.omnichat.auth.dto.IntrospectReq;
import com.omnichat.auth.dto.IntrospectRes;
import com.omnichat.auth.dto.LoginReq;
import com.omnichat.auth.dto.MessageRes;
import com.omnichat.auth.dto.RegisterReq;
import com.omnichat.auth.dto.TokenRes;
import com.omnichat.auth.dto.UserDto;

public interface AuthService {
    MessageRes register(RegisterReq request);
    MessageRes verifyEmail(String token);
    TokenRes login(LoginReq request);
    TokenRes googleLogin(com.omnichat.auth.dto.GoogleSsoReq request);
    TokenRes refresh(String refreshToken);
    void logout(String accessToken, String refreshToken, boolean allDevices);
    IntrospectRes introspect(IntrospectReq request);
    com.omnichat.auth.dto.UserProfileRes getCurrentProfile(String accessToken);
    void createOwnerAccount(com.omnichat.auth.dto.CreateOwnerReq request);
    void revokeTokensByEmails(java.util.List<String> emails);
}
