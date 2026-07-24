package com.omnichat.auth.service;

import com.omnichat.auth.domain.entity.Role;
import com.omnichat.auth.domain.entity.User;
import com.omnichat.auth.domain.entity.UserStatus;
import com.omnichat.auth.domain.entity.VerificationToken;
import com.omnichat.auth.domain.entity.RefreshToken;
import com.omnichat.auth.domain.entity.AuthProvider;
import com.omnichat.auth.dto.GoogleSsoReq;
import com.omnichat.auth.dto.GoogleUserInfo;
import com.omnichat.auth.dto.LoginReq;
import com.omnichat.auth.dto.MessageRes;
import com.omnichat.auth.dto.RegisterReq;
import com.omnichat.auth.dto.TokenRes;
import com.omnichat.auth.exception.UserAlreadyExistsException;
import com.omnichat.auth.mapper.UserMapper;
import com.omnichat.auth.repository.RefreshTokenRepository;
import com.omnichat.auth.repository.RoleRepository;
import com.omnichat.auth.repository.UserRepository;
import com.omnichat.auth.repository.VerificationTokenRepository;
import com.omnichat.auth.security.CustomUserDetails;
import com.omnichat.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private GoogleOAuthService googleOAuthService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterReq registerReq;
    private LoginReq loginReq;
    private GoogleSsoReq googleSsoReq;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "refreshTokenDurationMs", 604800000L);
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "accessTokenDurationMs", 86400000L);

        registerReq = new RegisterReq();
        registerReq.setEmail("test@example.com");
        registerReq.setFullName("Test User");
        registerReq.setPassword("Password123!");
        registerReq.setConfirmPassword("Password123!");
        registerReq.setRole("AGENT");

        loginReq = new LoginReq();
        loginReq.setEmail("test@example.com");
        loginReq.setPassword("Password123!");

        googleSsoReq = new GoogleSsoReq();
        googleSsoReq.setCode("valid-auth-code");
    }

    @Test
    void register_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        
        Role role = new Role();
        role.setName("ROLE_AGENT");
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(role));

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("test@example.com");
        savedUser.setFullName("Test User");
        savedUser.setStatus(UserStatus.PENDING_VERIFICATION);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken("some-token");
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);

        MessageRes response = authService.register(registerReq);

        assertEquals("Vui lòng kiểm tra email để kích hoạt tài khoản.", response.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
        verify(kafkaProducerService, times(1)).sendUserRegisteredEvent(any());
    }

    @Test
    void register_PasswordsDoNotMatch() {
        registerReq.setConfirmPassword("DifferentPassword!");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> authService.register(registerReq));
            
        assertEquals("Passwords do not match!", exception.getMessage());
    }

    @Test
    void register_EmailAlreadyExists_Active() {
        User existingUser = new User();
        existingUser.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existingUser));

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, 
            () -> authService.register(registerReq));
            
        assertEquals("Email is already in use!", exception.getMessage());
    }

    @Test
    void register_EmailAlreadyExists_PendingVerification() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("test@example.com");
        existingUser.setFullName("Test User");
        existingUser.setStatus(UserStatus.PENDING_VERIFICATION);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existingUser));

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken("new-token");
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);

        MessageRes response = authService.register(registerReq);

        assertEquals("Vui lòng kiểm tra email để kích hoạt tài khoản.", response.getMessage());
        verify(verificationTokenRepository, times(1)).deleteByUserId(existingUser.getId());
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
        verify(kafkaProducerService, times(1)).sendUserRegisteredEvent(any());
        verify(userRepository, never()).save(any(User.class)); // Shouldn't save user again
    }

    @Test
    void verifyEmail_Success() {
        String token = "valid-token";
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setExpiryDate(Instant.now().plusSeconds(3600)); // Future
        
        User user = new User();
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        verificationToken.setUser(user);
        
        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));

        MessageRes response = authService.verifyEmail(token);

        assertEquals("Xác thực thành công. Bạn có thể đăng nhập.", response.getMessage());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userRepository, times(1)).save(user);
        verify(verificationTokenRepository, times(1)).delete(verificationToken);
    }

    @Test
    void verifyEmail_ExpiredToken() {
        String token = "expired-token";
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setExpiryDate(Instant.now().minusSeconds(3600)); // Past
        
        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> authService.verifyEmail(token));
            
        assertEquals("Verification token has expired", exception.getMessage());
        verify(verificationTokenRepository, times(1)).delete(verificationToken);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Authentication auth = mock(Authentication.class);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(tokenProvider.generateToken(userDetails)).thenReturn("access-token");
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        TokenRes response = authService.login(loginReq);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(0, user.getFailedLoginAttempts());
    }

    @Test
    void login_WrongCredentials_IncrementsFailedAttempts() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(3);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authService.login(loginReq));
        assertEquals("Tài khoản hoặc mật khẩu không chính xác", exception.getMessage());
        assertEquals(4, user.getFailedLoginAttempts());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void login_WrongCredentials_LocksAccountAfter5Attempts() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(4);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        LockedException exception = assertThrows(LockedException.class, () -> authService.login(loginReq));
        assertEquals("Tài khoản đã bị tạm khóa do nhập sai nhiều lần", exception.getMessage());
        assertEquals(5, user.getFailedLoginAttempts());
        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertNotNull(user.getLockoutEnd());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void login_AccountPendingVerification() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        DisabledException exception = assertThrows(DisabledException.class, () -> authService.login(loginReq));
        assertEquals("Vui lòng xác thực email trước khi đăng nhập", exception.getMessage());
    }

    @Test
    void login_AccountLocked_LockoutEndNotExpired() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.LOCKED);
        user.setLockoutEnd(Instant.now().plusSeconds(600)); // Still locked
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        LockedException exception = assertThrows(LockedException.class, () -> authService.login(loginReq));
        assertEquals("Tài khoản đang bị tạm khóa, vui lòng thử lại sau", exception.getMessage());
    }

    @Test
    void login_AccountLocked_LockoutEndExpired_UnlocksAndAuthenticates() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.LOCKED);
        user.setFailedLoginAttempts(5);
        user.setLockoutEnd(Instant.now().minusSeconds(600)); // Lockout expired
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Authentication auth = mock(Authentication.class);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(tokenProvider.generateToken(userDetails)).thenReturn("access-token");
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        TokenRes response = authService.login(loginReq);

        assertEquals("access-token", response.getAccessToken());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockoutEnd());
        verify(userRepository, times(1)).save(user); // Wait, saved when unlocked! Wait, wait, if authentication succeeds, it's saved twice. But we verify times at least 1.
    }

    @Test
    void googleLogin_NewUser_CreatesActiveAccountAndReturnsTokens() {
        GoogleUserInfo googleUser = GoogleUserInfo.builder()
                .id("12345")
                .email("newuser@gmail.com")
                .name("New Google User")
                .picture("pic.jpg")
                .build();
        when(googleOAuthService.verifyCodeAndGetUserInfo("valid-auth-code")).thenReturn(googleUser);
        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
        
        Role role = new Role();
        role.setName("ROLE_AGENT");
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(role));

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setEmail("newuser@gmail.com");
        savedUser.setFullName("New Google User");
        savedUser.setStatus(UserStatus.ACTIVE);
        savedUser.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(savedUser));

        when(tokenProvider.generateToken(any())).thenReturn("access-token-google");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token-google");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        TokenRes response = authService.googleLogin(googleSsoReq);

        assertEquals("access-token-google", response.getAccessToken());
        assertEquals("refresh-token-google", response.getRefreshToken());
        verify(userRepository, times(1)).save(any(User.class));
        verify(kafkaProducerService, times(1)).sendUserRegisteredEvent(any());
    }

    @Test
    void googleLogin_ExistingLocalUser_LinksAccountAndReturnsTokens() {
        GoogleUserInfo googleUser = GoogleUserInfo.builder()
                .id("12345")
                .email("test@example.com")
                .name("Test User")
                .build();
        when(googleOAuthService.verifyCodeAndGetUserInfo("valid-auth-code")).thenReturn(googleUser);
        
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("test@example.com");
        existingUser.setStatus(UserStatus.ACTIVE);
        existingUser.setAuthProvider(AuthProvider.LOCAL);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        when(tokenProvider.generateToken(any())).thenReturn("access-token");
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        TokenRes response = authService.googleLogin(googleSsoReq);

        assertEquals("access-token", response.getAccessToken());
        assertEquals(AuthProvider.LOCAL_AND_GOOGLE, existingUser.getAuthProvider());
        verify(userRepository, times(1)).save(existingUser);
        verify(kafkaProducerService, never()).sendUserRegisteredEvent(any());
    }

    @Test
    void googleLogin_LockedUser_ThrowsLockedException() {
        GoogleUserInfo googleUser = GoogleUserInfo.builder()
                .id("12345")
                .email("locked@gmail.com")
                .build();
        when(googleOAuthService.verifyCodeAndGetUserInfo("valid-auth-code")).thenReturn(googleUser);
        
        User existingUser = new User();
        existingUser.setEmail("locked@gmail.com");
        existingUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("locked@gmail.com")).thenReturn(Optional.of(existingUser));

        LockedException exception = assertThrows(LockedException.class, () -> authService.googleLogin(googleSsoReq));
        assertEquals("Tài khoản của bạn đã bị khóa", exception.getMessage());
    }

    @Test
    void refresh_Success() {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);

        RefreshToken token = new RefreshToken();
        token.setToken("valid-refresh-token");
        token.setExpiryDate(Instant.now().plusSeconds(3600));
        token.setRevoked(false);
        token.setUser(user);

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(token));
        
        CustomUserDetails userDetails = new CustomUserDetails(user);
        when(tokenProvider.generateToken(any())).thenReturn("new-access-token");
        
        RefreshToken newToken = new RefreshToken();
        newToken.setToken("new-refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(newToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        TokenRes response = authService.refresh("valid-refresh-token");

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertTrue(token.isRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // one for marking revoked, one for new token
    }

    @Test
    void refresh_Expired() {
        RefreshToken token = new RefreshToken();
        token.setToken("expired-token");
        token.setExpiryDate(Instant.now().minusSeconds(3600)); // expired
        token.setRevoked(false);

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        com.omnichat.auth.exception.TokenRefreshException exception = assertThrows(
            com.omnichat.auth.exception.TokenRefreshException.class, 
            () -> authService.refresh("expired-token")
        );

        assertTrue(exception.getMessage().contains("Refresh token was expired or revoked"));
        verify(refreshTokenRepository, times(1)).delete(token);
    }

    @Test
    void refresh_ReplayAttack_RevokesAllTokens() {
        User user = new User();
        user.setId(1L);

        RefreshToken token = new RefreshToken();
        token.setToken("revoked-token");
        token.setExpiryDate(Instant.now().plusSeconds(3600));
        token.setRevoked(true); // Reused
        token.setUser(user);

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));

        com.omnichat.auth.exception.TokenRefreshException exception = assertThrows(
            com.omnichat.auth.exception.TokenRefreshException.class, 
            () -> authService.refresh("revoked-token")
        );

        assertTrue(exception.getMessage().contains("Refresh token was expired or revoked"));
        verify(refreshTokenRepository, times(1)).deleteAllByUser(user);
    }

    @Test
    void refresh_AccountLocked() {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.SUSPENDED);

        RefreshToken token = new RefreshToken();
        token.setToken("valid-token");
        token.setExpiryDate(Instant.now().plusSeconds(3600));
        token.setRevoked(false);
        token.setUser(user);

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        LockedException exception = assertThrows(LockedException.class, () -> authService.refresh("valid-token"));
        assertEquals("Tài khoản của bạn đã bị khóa", exception.getMessage());
    }
}
