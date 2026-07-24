package com.omnichat.auth.service;

import com.omnichat.auth.domain.entity.Role;
import com.omnichat.auth.domain.entity.User;
import com.omnichat.auth.domain.entity.UserStatus;
import com.omnichat.auth.domain.entity.VerificationToken;
import com.omnichat.auth.dto.MessageRes;
import com.omnichat.auth.dto.RegisterReq;
import com.omnichat.auth.exception.UserAlreadyExistsException;
import com.omnichat.auth.mapper.UserMapper;
import com.omnichat.auth.repository.RefreshTokenRepository;
import com.omnichat.auth.repository.RoleRepository;
import com.omnichat.auth.repository.UserRepository;
import com.omnichat.auth.repository.VerificationTokenRepository;
import com.omnichat.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
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

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterReq registerReq;

    @BeforeEach
    void setUp() {
        registerReq = new RegisterReq();
        registerReq.setEmail("test@example.com");
        registerReq.setFullName("Test User");
        registerReq.setPassword("Password123!");
        registerReq.setConfirmPassword("Password123!");
        registerReq.setRole("AGENT");
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
}
