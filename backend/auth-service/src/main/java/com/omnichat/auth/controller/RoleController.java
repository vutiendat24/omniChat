package com.omnichat.auth.controller;

import com.omnichat.auth.dto.RoleReq;
import com.omnichat.auth.dto.RoleRes;
import com.omnichat.auth.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<Page<RoleRes>> getRoles(@RequestParam(value = "name", required = false) String name,
                                                  Pageable pageable) {
        return ResponseEntity.ok(roleService.getRoles(name, pageable));
    }

    @PostMapping
    public ResponseEntity<RoleRes> createRole(@Valid @RequestBody RoleReq request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleRes> updateRole(@PathVariable("id") Long id,
                                              @Valid @RequestBody RoleReq request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
