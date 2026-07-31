package com.omnichat.user.controller;

import com.omnichat.user.domain.entity.User;
import com.omnichat.user.domain.entity.UserStatus;
import com.omnichat.user.dto.ErrorResponse;
import com.omnichat.user.dto.UpdatePasswordReq;
import com.omnichat.user.dto.UpdateProfileReq;
import com.omnichat.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PutMapping
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileReq req, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.SUSPENDED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        user.setFullName(req.getFullName());
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }
        userRepository.save(user);
        
        // Push event to Kafka
        Map<String, Object> event = new HashMap<>();
        event.put("userId", user.getId());
        event.put("fullName", user.getFullName());
        event.put("avatarUrl", user.getAvatarUrl());
        kafkaTemplate.send("omnichat.user.events", "UserProfileUpdatedEvent", event);

        return ResponseEntity.ok(user);
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@Valid @RequestBody UpdatePasswordReq req, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.SUSPENDED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (req.getOldPassword().equals(req.getNewPassword())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Mật khẩu mới không được trùng mật khẩu cũ"));
        }

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) {
                user.setStatus(UserStatus.LOCKED);
            }
            userRepository.save(user);
            return ResponseEntity.badRequest().body(new ErrorResponse("Mật khẩu cũ không chính xác"));
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }
}
