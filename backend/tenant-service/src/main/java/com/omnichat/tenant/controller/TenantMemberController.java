package com.omnichat.tenant.controller;

import com.omnichat.tenant.dto.InviteMemberReq;
import com.omnichat.tenant.dto.TenantMemberRes;
import com.omnichat.tenant.service.TenantMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/members")
@RequiredArgsConstructor
public class TenantMemberController {

    private final TenantMemberService tenantMemberService;

    @PostMapping("/invite")
    public ResponseEntity<TenantMemberRes> inviteMember(
            @PathVariable String tenantId,
            @Valid @RequestBody InviteMemberReq request) {
        return ResponseEntity.ok(tenantMemberService.inviteMember(tenantId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId,
            @PathVariable String tenantId,
            @PathVariable String userId) {
        tenantMemberService.removeMember(currentUserId, tenantId, userId);
        return ResponseEntity.noContent().build();
    }
}
