package com.omnichat.tenant.controller;

import com.omnichat.tenant.dto.BusinessHoursReq;
import com.omnichat.tenant.dto.BusinessHoursRes;
import com.omnichat.tenant.service.BusinessHoursService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/business-hours")
@RequiredArgsConstructor
public class BusinessHoursController {

    private final BusinessHoursService businessHoursService;

    @GetMapping
    public ResponseEntity<BusinessHoursRes> getBusinessHours(@PathVariable String tenantId) {
        return ResponseEntity.ok(businessHoursService.getBusinessHours(tenantId));
    }

    @PutMapping
    public ResponseEntity<BusinessHoursRes> updateBusinessHours(
            @PathVariable String tenantId,
            @Valid @RequestBody BusinessHoursReq request) {
        return ResponseEntity.ok(businessHoursService.updateBusinessHours(tenantId, request));
    }

    @PatchMapping
    public ResponseEntity<BusinessHoursRes> patchBusinessHours(
            @PathVariable String tenantId,
            @Valid @RequestBody BusinessHoursReq request) {
        return ResponseEntity.ok(businessHoursService.updateBusinessHours(tenantId, request));
    }
}
