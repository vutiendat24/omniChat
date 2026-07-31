package com.omnichat.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.user.domain.entity.User;
import com.omnichat.user.domain.entity.UserStatus;
import com.omnichat.user.dto.UpdatePasswordReq;
import com.omnichat.user.dto.UpdateProfileReq;
import com.omnichat.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = new User();
        testUser.setEmail("agent@omnichat.com");
        testUser.setFullName("Old Name");
        testUser.setAvatarUrl("http://old-avatar.com/img.png");
        testUser.setPassword(passwordEncoder.encode("OldPassword123!"));
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setFailedLoginAttempts(0);
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "agent@omnichat.com", roles = {"AGENT"})
    void shouldUpdateProfileSuccessfully() throws Exception {
        UpdateProfileReq req = new UpdateProfileReq("New Name", "http://new-avatar.com/img.png");

        mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"))
                .andExpect(jsonPath("$.avatarUrl").value("http://new-avatar.com/img.png"));
        
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("New Name", updatedUser.getFullName());
    }

    @Test
    @WithMockUser(username = "agent@omnichat.com", roles = {"AGENT"})
    void shouldRejectXssInFullName() throws Exception {
        UpdateProfileReq req = new UpdateProfileReq("<script>alert(1)</script>", "http://new-avatar.com/img.png");

        mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "agent@omnichat.com", roles = {"AGENT"})
    void shouldRejectInvalidAvatarUrl() throws Exception {
        UpdateProfileReq req = new UpdateProfileReq("Valid Name", "invalid-url");

        mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "agent@omnichat.com", roles = {"AGENT"})
    void shouldUpdatePasswordSuccessfully() throws Exception {
        UpdatePasswordReq req = new UpdatePasswordReq("OldPassword123!", "NewPassword123@");

        mockMvc.perform(put("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(0, updatedUser.getFailedLoginAttempts());
    }

    @Test
    @WithMockUser(username = "agent@omnichat.com", roles = {"AGENT"})
    void shouldRejectPasswordUpdateWhenOldPasswordIsWrong() throws Exception {
        UpdatePasswordReq req = new UpdatePasswordReq("WrongOld123!", "NewPassword123@");

        mockMvc.perform(put("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu cũ không chính xác"));
        
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(1, updatedUser.getFailedLoginAttempts());
    }

    @Test
    @WithMockUser(username = "agent@omnichat.com", roles = {"AGENT"})
    void shouldLockAccountAfter5FailedPasswordAttempts() throws Exception {
        testUser.setFailedLoginAttempts(4);
        userRepository.save(testUser);

        UpdatePasswordReq req = new UpdatePasswordReq("WrongOld123!", "NewPassword123@");

        mockMvc.perform(put("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(UserStatus.LOCKED, updatedUser.getStatus());
        assertEquals(5, updatedUser.getFailedLoginAttempts());
    }

    @Test
    @WithMockUser(username = "locked@omnichat.com")
    void shouldRejectRequestWhenAccountIsSuspendedOrLocked() throws Exception {
        User lockedUser = new User();
        lockedUser.setEmail("locked@omnichat.com");
        lockedUser.setFullName("Locked");
        lockedUser.setPassword("Any");
        lockedUser.setStatus(UserStatus.LOCKED);
        userRepository.save(lockedUser);

        UpdateProfileReq req = new UpdateProfileReq("New Name", "http://new.com");

        mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "agent@omnichat.com", roles = {"AGENT"})
    void shouldRejectNewPasswordIfSameAsOld() throws Exception {
        UpdatePasswordReq req = new UpdatePasswordReq("OldPassword123!", "OldPassword123!");

        mockMvc.perform(put("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "agent@omnichat.com", roles = {"AGENT"})
    void shouldRejectWeakNewPassword() throws Exception {
        UpdatePasswordReq req = new UpdatePasswordReq("OldPassword123!", "weak");

        mockMvc.perform(put("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
