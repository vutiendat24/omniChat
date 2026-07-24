package com.omnichat.tenant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.tenant.domain.entity.OutboxEvent;
import com.omnichat.tenant.domain.entity.Plan;
import com.omnichat.tenant.domain.entity.*;
import com.omnichat.tenant.dto.*;
import com.omnichat.tenant.exception.DuplicateResourceException;
import com.omnichat.tenant.exception.QuotaExceededException;
import com.omnichat.tenant.exception.ResourceNotFoundException;
import com.omnichat.tenant.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final UserTeamRepository userTeamRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TeamRes createTeam(String tenantId, CreateTeamReq request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Plan plan = planRepository.findById(tenant.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        if (plan.getMaxTeams() != null && plan.getMaxTeams() != -1) {
            long currentTeams = teamRepository.countByTenantId(tenantId);
            if (currentTeams >= plan.getMaxTeams()) {
                throw new QuotaExceededException("Bạn đã đạt giới hạn số lượng nhóm của gói cước hiện tại. Vui lòng nâng cấp gói");
            }
        }

        if (teamRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.getTeamName())) {
            throw new DuplicateResourceException("Tên nhóm đã tồn tại trong hệ thống");
        }

        Team team = Team.builder()
                .tenantId(tenantId)
                .name(request.getTeamName())
                .description(request.getDescription())
                .build();

        Team savedTeam = teamRepository.save(team);

        Map<String, Object> payload = new HashMap<>();
        payload.put("teamId", savedTeam.getId());
        payload.put("tenantId", tenantId);
        payload.put("teamName", savedTeam.getName());
        payload.put("createdAt", savedTeam.getCreatedAt());

        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Team")
                    .aggregateId(savedTeam.getId())
                    .type("team.created")
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }

        return TeamRes.builder()
                .teamId(savedTeam.getId())
                .teamName(savedTeam.getName())
                .description(savedTeam.getDescription())
                .createdAt(savedTeam.getCreatedAt())
                .build();
    }

    @Transactional
    public TeamRes updateTeam(String tenantId, String teamId, UpdateTeamReq request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm không tồn tại hoặc đã bị xóa"));

        if (!team.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Nhóm không tồn tại hoặc đã bị xóa");
        }

        if (teamRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, request.getTeamName(), teamId)) {
            throw new DuplicateResourceException("Tên nhóm đã tồn tại, vui lòng chọn tên khác");
        }

        team.setName(request.getTeamName());
        team.setDescription(request.getDescription());
        // Set version for optimistic locking. Spring Data JPA handles the check during save/flush
        team.setVersion(request.getVersion());

        Team savedTeam = teamRepository.save(team);

        Map<String, Object> payload = new HashMap<>();
        payload.put("teamId", savedTeam.getId());
        payload.put("tenantId", tenantId);
        payload.put("updatedFields", Map.of("name", savedTeam.getName(), "description", savedTeam.getDescription()));
        payload.put("updatedAt", savedTeam.getUpdatedAt());

        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Team")
                    .aggregateId(savedTeam.getId())
                    .type("team.updated")
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }

        return TeamRes.builder()
                .teamId(savedTeam.getId())
                .teamName(savedTeam.getName())
                .description(savedTeam.getDescription())
                .createdAt(savedTeam.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteTeam(String tenantId, String teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm không tồn tại hoặc đã bị xóa"));

        if (!team.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Nhóm không tồn tại hoặc đã bị xóa");
        }

        long teamCount = teamRepository.countByTenantId(tenantId);
        if (teamCount <= 1) {
            throw new IllegalArgumentException("Không thể xóa nhóm mặc định của cửa hàng");
        }

        teamRepository.delete(team);

        Map<String, Object> payload = new HashMap<>();
        payload.put("teamId", teamId);
        payload.put("tenantId", tenantId);
        payload.put("deletedAt", java.time.LocalDateTime.now());

        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Team")
                    .aggregateId(teamId)
                    .type("team.deleted")
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }

    @Transactional
    public AssignMemberRes assignMembers(String tenantId, String teamId, AssignMemberReq request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new IllegalArgumentException("Danh sách userIds không được để trống");
        }
        if (request.getUserIds().size() > 50) {
            throw new IllegalArgumentException("Chỉ được gán tối đa 50 thành viên trong một thao tác");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team không tồn tại hoặc không thuộc tenant này"));
        
        if (!team.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Team không tồn tại hoặc không thuộc tenant này");
        }

        List<TenantMember> members = tenantMemberRepository.findByIdInAndTenantId(request.getUserIds(), tenantId);
        
        if (members.size() != request.getUserIds().size()) {
            throw new IllegalArgumentException("Một số thành viên không thuộc cửa hàng này");
        }

        for (TenantMember member : members) {
            if (member.getStatus() == TenantMemberStatus.PENDING) {
                throw new IllegalArgumentException("Chỉ có thể gán các thành viên đã kích hoạt tài khoản");
            }
            if (member.getStatus() == TenantMemberStatus.INACTIVE) {
                throw new IllegalArgumentException("Không thể gán thành viên đang bị khóa");
            }
        }

        Set<String> existingUserIds = userTeamRepository.findExistingUserIdsInTeam(teamId, request.getUserIds());
        
        int ignoredCount = existingUserIds.size();
        int addedCount = 0;
        List<UserTeam> newAssignments = new java.util.ArrayList<>();

        for (String userId : request.getUserIds()) {
            if (!existingUserIds.contains(userId)) {
                UserTeamId utId = new UserTeamId(userId, teamId);
                newAssignments.add(UserTeam.builder().id(utId).build());
                addedCount++;
            }
        }

        if (!newAssignments.isEmpty()) {
            userTeamRepository.saveAll(newAssignments);

            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", tenantId);
            payload.put("teamId", teamId);
            payload.put("userIds", newAssignments.stream().map(ut -> ut.getId().getUserId()).collect(Collectors.toList()));
            payload.put("assignedAt", LocalDateTime.now().toString());

            try {
                OutboxEvent event = OutboxEvent.builder()
                        .aggregateType("Team")
                        .aggregateId(teamId)
                        .type("team.members_assigned")
                        .payload(objectMapper.writeValueAsString(payload))
                        .build();
                outboxEventRepository.save(event);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize outbox event payload", e);
                throw new RuntimeException("Failed to serialize outbox event payload", e);
            }
        }

        return AssignMemberRes.builder()
                .message("Gán thành viên thành công")
                .addedCount(addedCount)
                .ignoredCount(ignoredCount)
                .build();
    }
}
