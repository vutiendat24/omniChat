package com.omnichat.user.controller;

import com.omnichat.user.domain.entity.Role;
import com.omnichat.user.domain.entity.User;
import com.omnichat.user.domain.entity.WorkspaceMember;
import com.omnichat.user.dto.ErrorResponse;
import com.omnichat.user.dto.RoleReq;
import com.omnichat.user.repository.RoleRepository;
import com.omnichat.user.repository.UserRepository;
import com.omnichat.user.repository.WorkspaceMemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{id}/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getRoles(@PathVariable("id") Long workspaceId, Authentication authentication) {
        List<Role> roles = roleRepository.findByWorkspaceId(workspaceId);
        return ResponseEntity.ok(roles);
    }

    @PostMapping
    public ResponseEntity<?> createRole(@PathVariable("id") Long workspaceId,
                                        @Valid @RequestBody RoleReq req,
                                        Authentication authentication) {
        String actorEmail = authentication.getName();
        User actorUser = userRepository.findByEmail(actorEmail).orElse(null);
        if (actorUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WorkspaceMember actorMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorUser.getId()).orElse(null);
        if (actorMember == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (req.getLevel() >= actorMember.getRole().getLevel()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Level của Role mới phải nhỏ hơn Level của bạn"));
        }

        if (roleRepository.existsByWorkspaceIdAndName(workspaceId, req.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Tên Role đã tồn tại trong Workspace"));
        }

        Role newRole = Role.builder()
                .workspaceId(workspaceId)
                .name(req.getName())
                .description(req.getDescription())
                .level(req.getLevel())
                .isSystem(false)
                .build();

        try {
            roleRepository.save(newRole);
            return ResponseEntity.status(HttpStatus.CREATED).body(newRole);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Tên Role đã tồn tại trong Workspace"));
        }
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<?> updateRole(@PathVariable("id") Long workspaceId,
                                        @PathVariable("roleId") Long roleId,
                                        @Valid @RequestBody RoleReq req,
                                        Authentication authentication) {
        String actorEmail = authentication.getName();
        User actorUser = userRepository.findByEmail(actorEmail).orElse(null);
        if (actorUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WorkspaceMember actorMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorUser.getId()).orElse(null);
        if (actorMember == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Role role = roleRepository.findById(roleId).orElse(null);
        if (role == null || !role.getWorkspaceId().equals(workspaceId)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Role không tồn tại"));
        }

        if (role.isSystem()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Không thể sửa Role hệ thống"));
        }

        if (req.getLevel() >= actorMember.getRole().getLevel()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Level của Role phải nhỏ hơn Level của bạn"));
        }

        if (!role.getName().equals(req.getName()) && roleRepository.existsByWorkspaceIdAndName(workspaceId, req.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Tên Role đã tồn tại trong Workspace"));
        }

        role.setName(req.getName());
        role.setDescription(req.getDescription());
        role.setLevel(req.getLevel());

        try {
            roleRepository.save(role);
            return ResponseEntity.ok(role);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Tên Role đã tồn tại trong Workspace"));
        }
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<?> deleteRole(@PathVariable("id") Long workspaceId,
                                        @PathVariable("roleId") Long roleId,
                                        Authentication authentication) {
        String actorEmail = authentication.getName();
        User actorUser = userRepository.findByEmail(actorEmail).orElse(null);
        if (actorUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WorkspaceMember actorMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorUser.getId()).orElse(null);
        if (actorMember == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Role role = roleRepository.findById(roleId).orElse(null);
        if (role == null || !role.getWorkspaceId().equals(workspaceId)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Role không tồn tại"));
        }

        if (role.isSystem()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Không thể xóa Role hệ thống"));
        }

        long usersCount = workspaceMemberRepository.countByRoleId(roleId);
        if (usersCount > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Không thể xóa Role đang có người sử dụng. Yêu cầu chuyển Role của các thành viên sang Role khác trước khi xóa."));
        }

        roleRepository.delete(role);
        return ResponseEntity.ok().build();
    }
}
