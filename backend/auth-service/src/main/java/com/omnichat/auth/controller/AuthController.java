package com.omnichat.auth.controller;

import com.omnichat.auth.dto.*;
import com.omnichat.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<MessageRes> register(@Valid @RequestBody RegisterReq request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<MessageRes> verifyEmail(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenRes> login(@Valid @RequestBody LoginReq request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRes> refresh(@RequestParam("token") String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String accessToken,
                                       @RequestParam(value = "refreshToken", required = false) String refreshToken) {
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/introspect")
    public ResponseEntity<IntrospectRes> introspect(@Valid @RequestBody IntrospectReq request) {
        return ResponseEntity.ok(authService.introspect(request));
    }
}
