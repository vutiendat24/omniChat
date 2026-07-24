package com.omnichat.auth.service.implement;

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
import com.omnichat.auth.service.AuthService;
import com.omnichat.auth.service.GoogleOAuthService;
import com.omnichat.auth.service.KafkaProducerService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.omnichat.auth.domain.entity.UserStatus;
import com.omnichat.auth.domain.entity.VerificationToken;
import com.omnichat.auth.domain.entity.AuthProvider;
import com.omnichat.auth.repository.VerificationTokenRepository;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final KafkaProducerService kafkaProducerService;
    private final GoogleOAuthService googleOAuthService;

    @Value("${jwt.refresh-expiration-ms}")
    private Long refreshTokenDurationMs;

    @Value("${jwt.expiration-ms}")
    private Long accessTokenDurationMs;

    @Override
    @Transactional
    public MessageRes register(RegisterReq request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match!");
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getStatus() == UserStatus.ACTIVE) {
                throw new UserAlreadyExistsException("Email is already in use!");
            } else if (existingUser.getStatus() == UserStatus.PENDING_VERIFICATION) {
                // Resend verification email
                verificationTokenRepository.deleteByUserId(existingUser.getId());
                VerificationToken newToken = createVerificationToken(existingUser);
                kafkaProducerService.sendUserRegisteredEvent(new UserRegisteredEvent(
                        existingUser.getEmail(), existingUser.getFullName(), newToken.getToken()));
                return new MessageRes("Vui lòng kiểm tra email để kích hoạt tài khoản.");
            }
        }

        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.PENDING_VERIFICATION)
                .authProvider(AuthProvider.LOCAL)
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
        VerificationToken verificationToken = createVerificationToken(savedUser);

        kafkaProducerService.sendUserRegisteredEvent(new UserRegisteredEvent(
                savedUser.getEmail(), savedUser.getFullName(), verificationToken.getToken()));

        return new MessageRes("Vui lòng kiểm tra email để kích hoạt tài khoản.");
    }

    @Override
    @Transactional
    public MessageRes verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (verificationToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            verificationTokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        verificationTokenRepository.delete(verificationToken);

        return new MessageRes("Xác thực thành công. Bạn có thể đăng nhập.");
    }

    private VerificationToken createVerificationToken(User user) {
        VerificationToken token = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusSeconds(86400)) // 24 hours
                .build();
        return verificationTokenRepository.save(token);
    }

    @Override
    @Transactional
    public TokenRes login(LoginReq request) {
        if (request.getEmail() != null) {
            request.setEmail(request.getEmail().trim());
        }
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            throw new org.springframework.security.authentication.BadCredentialsException("Tài khoản hoặc mật khẩu không chính xác");
        }
        User user = userOpt.get();

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new org.springframework.security.authentication.DisabledException("Vui lòng xác thực email trước khi đăng nhập");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new org.springframework.security.authentication.LockedException("Tài khoản của bạn đã bị khóa");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            if (user.getLockoutEnd() != null && user.getLockoutEnd().compareTo(Instant.now()) > 0) {
                throw new org.springframework.security.authentication.LockedException("Tài khoản đang bị tạm khóa, vui lòng thử lại sau");
            } else {
                user.setStatus(UserStatus.ACTIVE);
                user.setFailedLoginAttempts(0);
                user.setLockoutEnd(null);
                userRepository.save(user);
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String accessToken = tokenProvider.generateToken(userDetails);
            RefreshToken refreshToken = createRefreshToken(userDetails.getUser().getId());

            return TokenRes.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .expiresIn(accessTokenDurationMs)
                    .build();
        } catch (org.springframework.security.core.AuthenticationException e) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockoutEnd(Instant.now().plusSeconds(1800)); // 30 mins
                userRepository.save(user);
                throw new org.springframework.security.authentication.LockedException("Tài khoản đã bị tạm khóa do nhập sai nhiều lần");
            }
            userRepository.save(user);
            throw new org.springframework.security.authentication.BadCredentialsException("Tài khoản hoặc mật khẩu không chính xác");
        }
    }

    @Override
    @Transactional
    public TokenRes googleLogin(GoogleSsoReq request) {
        GoogleUserInfo googleUserInfo = googleOAuthService.verifyCodeAndGetUserInfo(request.getCode());
        String email = googleUserInfo.getEmail();

        Optional<User> userOpt = userRepository.findByEmail(email);
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (user.getStatus() == UserStatus.SUSPENDED) {
                throw new org.springframework.security.authentication.LockedException("Tài khoản của bạn đã bị khóa");
            }
            if (user.getStatus() == UserStatus.LOCKED) {
                if (user.getLockoutEnd() != null && user.getLockoutEnd().compareTo(Instant.now()) > 0) {
                    throw new org.springframework.security.authentication.LockedException("Tài khoản đang bị tạm khóa, vui lòng thử lại sau");
                } else {
                    user.setStatus(UserStatus.ACTIVE);
                    user.setFailedLoginAttempts(0);
                    user.setLockoutEnd(null);
                }
            }
            if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                user.setStatus(UserStatus.ACTIVE);
            }
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                user.setAuthProvider(AuthProvider.LOCAL_AND_GOOGLE);
            }
            user = userRepository.save(user);
        } else {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(googleUserInfo.getName());
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            
            Role userRole = roleRepository.findByName("ROLE_AGENT")
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            newUser.setRoles(roles);
            
            user = userRepository.save(newUser);
            
            // publish event
            UserRegisteredEvent event = new UserRegisteredEvent(user.getEmail(), user.getFullName(), null);
            kafkaProducerService.sendUserRegisteredEvent(event);
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = tokenProvider.generateToken(userDetails);
        RefreshToken refreshToken = createRefreshToken(user.getId());

        return TokenRes.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(accessTokenDurationMs)
                .build();
    }

    @Override
    @Transactional
    public TokenRes refresh(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException(token, "Refresh token is not in database!"));

        if (refreshToken.isRevoked()) {
            // Replay attack!
            refreshTokenRepository.deleteAllByUser(refreshToken.getUser());
            throw new TokenRefreshException(token, "Refresh token was expired or revoked. Please make a new signin request");
        }

        if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException(token, "Refresh token was expired or revoked. Please make a new signin request");
        }

        User user = refreshToken.getUser();
        if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.LOCKED) {
            throw new org.springframework.security.authentication.LockedException("Tài khoản của bạn đã bị khóa");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = tokenProvider.generateToken(userDetails);
        RefreshToken newRefreshToken = createRefreshToken(user.getId());

        return TokenRes.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken.getToken())
                .expiresIn(accessTokenDurationMs)
                .build();
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken, boolean allDevices) {
        Long userId = null;

        if (accessToken != null) {
            tokenProvider.blacklistToken(accessToken);
            userId = tokenProvider.getUserIdFromToken(accessToken);
            
            if (userId != null) {
                com.omnichat.auth.dto.TokenBlacklistedEvent event = new com.omnichat.auth.dto.TokenBlacklistedEvent(userId, accessToken);
                kafkaProducerService.sendTokenBlacklistedEvent(event);
            }
        }

        if (allDevices && userId != null) {
            refreshTokenRepository.deleteByUser_Id(userId);
        } else if (refreshToken != null) {
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

    @Override
    public com.omnichat.auth.dto.UserProfileRes getCurrentProfile(String accessToken) {
        if (!tokenProvider.validateToken(accessToken)) {
            throw new org.springframework.security.authentication.BadCredentialsException("Token không hợp lệ hoặc đã hết hạn");
        }

        Long userId = tokenProvider.getUserIdFromToken(accessToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Không tìm thấy người dùng"));

        if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.LOCKED) {
            throw new org.springframework.security.authentication.LockedException("Tài khoản của bạn đã bị khóa");
        }

        return com.omnichat.auth.dto.UserProfileRes.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .build();
    }

    private RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(userRepository.findById(userId).orElseThrow());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void createOwnerAccount(com.omnichat.auth.dto.CreateOwnerReq request) {
        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
        } else {
            user = User.builder()
                    .email(request.getEmail())
                    .fullName(request.getFullName())
                    // Auto generate a random password, they can reset it later, or login via SSO
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .status(UserStatus.ACTIVE) // Auto active for tenant owner
                    .authProvider(AuthProvider.LOCAL)
                    .build();
        }

        Role ownerRole = roleRepository.findByName("ROLE_TENANT_OWNER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_TENANT_OWNER").description("Tenant Owner").build()));
        
        user.getRoles().add(ownerRole);
        userRepository.save(user);

        // Note: Currently auth-service doesn't store tenantId mapping per user in DB. 
        // We just assign the ROLE_TENANT_OWNER for now as per simple setup.
    }

    @Override
    @Transactional
    public void revokeTokensByEmails(java.util.List<String> emails) {
        if (emails == null || emails.isEmpty()) return;
        
        for (String email : emails) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                Long userId = userOpt.get().getId();
                // We invalidate refresh tokens. 
                refreshTokenRepository.deleteByUser_Id(userId);
                // Note: In a fully fleshed out system, we would also blacklist all active access tokens
                // by pushing an event to Redis or Kafka. Since we don't track all active access tokens 
                // individually in DB, we rely on the short TTL of access tokens or event publishing.
                com.omnichat.auth.dto.TokenBlacklistedEvent event = new com.omnichat.auth.dto.TokenBlacklistedEvent(userId, "ALL_TOKENS_REVOKED");
                kafkaProducerService.sendTokenBlacklistedEvent(event);
            }
        }
    }

    @Override
    @Transactional
    public void inviteUser(com.omnichat.auth.dto.InviteUserReq request) {
        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());

        if (existingUserOpt.isEmpty()) {
            User user = User.builder()
                    .email(request.getEmail())
                    .fullName(request.getEmail().split("@")[0]) // Default name
                    .password(null) // No password, must set via link
                    .status(UserStatus.PENDING_VERIFICATION)
                    .authProvider(AuthProvider.LOCAL)
                    .build();

            Role agentRole = roleRepository.findByName("ROLE_AGENT")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_AGENT").description("Agent").build()));
            
            user.getRoles().add(agentRole);
            userRepository.save(user);
        }
    }
}
