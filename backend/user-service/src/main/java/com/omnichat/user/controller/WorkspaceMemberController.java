package com.omnichat.user.controller;

import com.omnichat.user.domain.entity.*;
import com.omnichat.user.dto.ErrorResponse;
import com.omnichat.user.dto.InviteMemberReq;
import com.omnichat.user.repository.RoleRepository;
import com.omnichat.user.repository.UserRepository;
import com.omnichat.user.repository.WorkspaceMemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/workspaces/{id}/members")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping("/invite")
    public ResponseEntity<?> inviteMember(@PathVariable("id") Long workspaceId,
                                          @Valid @RequestBody InviteMemberReq req,
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

        Role targetRole = roleRepository.findById(req.getRoleId()).orElse(null);
        if (targetRole == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Role không tồn tại"));
        }

        if (targetRole.getLevel() >= 100) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Không thể mời Owner"));
        }

        if (actorMember.getRole().getLevel() <= targetRole.getLevel()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Không đủ quyền để mời cấp bậc này"));
        }

        User targetUser = userRepository.findByEmail(req.getEmail()).orElseGet(() -> {
            User newUser = User.builder()
                    .email(req.getEmail())
                    .status(UserStatus.INACTIVE)
                    .build();
            return userRepository.save(newUser);
        });

        Optional<WorkspaceMember> existingMemberOpt = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUser.getId());
        if (existingMemberOpt.isPresent()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Thành viên đã tồn tại trong workspace"));
        }

        WorkspaceMember newMember = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .user(targetUser)
                .role(targetRole)
                .status(MemberStatus.PENDING)
                .build();
        workspaceMemberRepository.save(newMember);

        Map<String, Object> event = new HashMap<>();
        event.put("workspaceId", workspaceId);
        event.put("email", req.getEmail());
        event.put("roleId", req.getRoleId());
        kafkaTemplate.send("omnichat.user.events", "MemberInvitedEvent", event);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable("id") Long workspaceId,
                                          @PathVariable("userId") Long targetUserId,
                                          Authentication authentication) {
        String actorEmail = authentication.getName();
        User actorUser = userRepository.findByEmail(actorEmail).orElse(null);
        if (actorUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (actorUser.getId().equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Không thể tự xóa chính mình"));
        }

        WorkspaceMember actorMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorUser.getId()).orElse(null);
        if (actorMember == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        WorkspaceMember targetMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId).orElse(null);
        if (targetMember == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Thành viên không tồn tại trong workspace"));
        }

        if (targetMember.getRole().getLevel() >= 100) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Không thể xóa Owner"));
        }

        if (actorMember.getRole().getLevel() <= targetMember.getRole().getLevel()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Không đủ quyền để xóa cấp bậc này"));
        }

        workspaceMemberRepository.delete(targetMember);

        Map<String, Object> event = new HashMap<>();
        event.put("workspaceId", workspaceId);
        event.put("userId", targetUserId);
        kafkaTemplate.send("omnichat.user.events", "MemberRemovedEvent", event);

        return ResponseEntity.ok().build();
    }
}
