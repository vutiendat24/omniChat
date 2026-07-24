package com.omnichat.tenant.controller;

import com.omnichat.tenant.dto.CreateTenantReq;
import com.omnichat.tenant.dto.UpdateTenantReq;
import com.omnichat.tenant.dto.TenantRes;
import com.omnichat.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantRes> createTenant(@Valid @RequestBody CreateTenantReq request) {
        return ResponseEntity.status(201).body(tenantService.createTenant(request));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{tenantId}")
    public ResponseEntity<TenantRes> updateTenant(
            @org.springframework.web.bind.annotation.PathVariable String tenantId,
            @Valid @RequestBody UpdateTenantReq request) {
        return ResponseEntity.ok(tenantService.updateTenant(tenantId, request));
    }
}
