package com.omnichat.auth.service;

import com.omnichat.auth.domain.entity.RefreshToken;
import com.omnichat.auth.domain.entity.Role;
import com.omnichat.auth.domain.entity.User;
import com.omnichat.auth.dto.*;
import com.omnichat.auth.exception.TokenRefreshException;
import com.omnichat.auth.exception.UserAlreadyExistsException;
import com.omnichat.auth.mapper.UserMapper;
import com.omnichat.auth.repository.RefreshTokenRepository;
import com.omnichat.auth.repository.RoleRepository;
import com.omnichat.auth.repository.UserRepository;
import com.omnichat.auth.security.CustomUserDetails;
import com.omnichat.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Value("${jwt.refresh-expiration-ms}")
    private Long refreshTokenDurationMs;

    @Value("${jwt.expiration-ms}")
    private Long accessTokenDurationMs;

    @Override
    @Transactional
    public UserDto register(RegisterReq request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        String roleName = request.getRole() != null ? request.getRole().toUpperCase() : "AGENT";
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isPresent()) {
            user.getRoles().add(roleOpt.get());
        }

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public TokenRes login(LoginReq request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken = tokenProvider.generateToken(userDetails);
        RefreshToken refreshToken = createRefreshToken(userDetails.getUser().getId());

        return TokenRes.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(accessTokenDurationMs)
                .build();
    }

    @Override
    @Transactional
    public TokenRes refresh(String token) {
        return refreshTokenRepository.findByToken(token)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = tokenProvider.generateToken(new CustomUserDetails(user));
                    return TokenRes.builder()
                            .accessToken(accessToken)
                            .refreshToken(token)
                            .expiresIn(accessTokenDurationMs)
                            .build();
                })
                .orElseThrow(() -> new TokenRefreshException(token, "Refresh token is not in database!"));
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // Blacklist the current access token in Redis
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            tokenProvider.blacklistToken(accessToken.substring(7));
        }

        // Revoke the refresh token in DB
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Override
    public IntrospectRes introspect(IntrospectReq request) {
        boolean isValid = tokenProvider.validateToken(request.getToken());
        return IntrospectRes.builder().valid(isValid).build();
    }

    private RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(userRepository.findById(userId).orElseThrow());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked() || token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired or revoked. Please make a new signin request");
        }
        return token;
    }
}
