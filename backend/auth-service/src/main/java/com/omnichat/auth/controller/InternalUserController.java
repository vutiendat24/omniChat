package com.omnichat.auth.controller;

import com.omnichat.auth.dto.CreateOwnerReq;
import com.omnichat.auth.dto.InviteUserReq;
import com.omnichat.auth.dto.RevokeTokensReq;
import com.omnichat.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final AuthService authService;

    @PostMapping("/owner")
    public ResponseEntity<Void> createOwnerAccount(@Valid @RequestBody CreateOwnerReq request) {
        authService.createOwnerAccount(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke-by-emails")
    public ResponseEntity<Void> revokeTokensByEmails(@Valid @RequestBody RevokeTokensReq request) {
        authService.revokeTokensByEmails(request.getEmails());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invite")
    public ResponseEntity<Void> inviteUser(@Valid @RequestBody InviteUserReq request) {
        authService.inviteUser(request);
        return ResponseEntity.ok().build();
    }
}
