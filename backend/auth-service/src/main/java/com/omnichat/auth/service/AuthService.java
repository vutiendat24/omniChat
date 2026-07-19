package com.omnichat.auth.service;

import com.omnichat.auth.dto.IntrospectReq;
import com.omnichat.auth.dto.IntrospectRes;
import com.omnichat.auth.dto.LoginReq;
import com.omnichat.auth.dto.RegisterReq;
import com.omnichat.auth.dto.TokenRes;
import com.omnichat.auth.dto.UserDto;

public interface AuthService {
    UserDto register(RegisterReq request);
    TokenRes login(LoginReq request);
    TokenRes refresh(String refreshToken);
    void logout(String accessToken, String refreshToken);
    IntrospectRes introspect(IntrospectReq request);
}
