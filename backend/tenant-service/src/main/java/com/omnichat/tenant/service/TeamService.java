package com.omnichat.tenant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.tenant.domain.entity.OutboxEvent;
import com.omnichat.tenant.domain.entity.Plan;
import com.omnichat.tenant.domain.entity.Team;
import com.omnichat.tenant.domain.entity.Tenant;
import com.omnichat.tenant.dto.CreateTeamReq;
import com.omnichat.tenant.dto.TeamRes;
import com.omnichat.tenant.exception.DuplicateResourceException;
import com.omnichat.tenant.exception.QuotaExceededException;
import com.omnichat.tenant.exception.ResourceNotFoundException;
import com.omnichat.tenant.repository.OutboxEventRepository;
import com.omnichat.tenant.repository.PlanRepository;
import com.omnichat.tenant.repository.TeamRepository;
import com.omnichat.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final OutboxEventRepository outboxEventRepository;
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
}
