package com.omnichat.auth.controller;

import com.omnichat.auth.dto.PermissionRes;
import com.omnichat.auth.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {
    
    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<PermissionRes>> getAllPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }
}
