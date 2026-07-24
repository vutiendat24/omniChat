package com.omnichat.tenant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.tenant.client.AuthServiceClient;
import com.omnichat.tenant.domain.entity.OutboxEvent;
import com.omnichat.tenant.domain.entity.Tenant;
import com.omnichat.tenant.domain.entity.TenantMember;
import com.omnichat.tenant.domain.entity.TenantMemberStatus;
import com.omnichat.tenant.domain.entity.Plan;
import com.omnichat.tenant.dto.InviteMemberReq;
import com.omnichat.tenant.dto.InviteUserReq;
import com.omnichat.tenant.dto.TenantMemberRes;
import com.omnichat.tenant.exception.DuplicateResourceException;
import com.omnichat.tenant.exception.QuotaExceededException;
import com.omnichat.tenant.exception.ResourceNotFoundException;
import com.omnichat.tenant.repository.OutboxEventRepository;
import com.omnichat.tenant.repository.PlanRepository;
import com.omnichat.tenant.repository.TenantMemberRepository;
import com.omnichat.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantMemberService {

    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final PlanRepository planRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuthServiceClient authServiceClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public TenantMemberRes inviteMember(String tenantId, InviteMemberReq req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (req.getEmail().equalsIgnoreCase(tenant.getOwnerEmail())) {
            throw new IllegalArgumentException("Bạn không thể tự mời chính mình vào không gian làm việc này");
        }

        Plan plan = null;
        if (tenant.getPlanId() != null) {
            plan = planRepository.findById(tenant.getPlanId()).orElse(null);
        }
        
        if (plan != null && plan.getMaxUsers() != null && plan.getMaxUsers() != -1) {
            long currentMembers = tenantMemberRepository.countByTenantIdAndStatusIn(
                    tenantId, List.of(TenantMemberStatus.ACTIVE, TenantMemberStatus.PENDING)
            );
            // +1 for the owner, but let's assume maxUsers is the limit for members + owner.
            // If the requirement means total users, we should count owner as well.
            // Let's count current members in the DB. The owner might not be in the DB yet unless we added them.
            // Let's assume currentMembers counts the invitees. We add 1 for owner if they are not in `tenant_members`.
            if (currentMembers + 1 >= plan.getMaxUsers()) {
                throw new QuotaExceededException("Đã đạt giới hạn số lượng thành viên của gói cước hiện tại");
            }
        }

        tenantMemberRepository.findByTenantIdAndEmail(tenantId, req.getEmail()).ifPresent(existingMember -> {
            if (existingMember.getStatus() == TenantMemberStatus.ACTIVE) {
                throw new DuplicateResourceException("Thành viên đã tồn tại trong không gian làm việc");
            }
            if (existingMember.getStatus() == TenantMemberStatus.PENDING) {
                throw new DuplicateResourceException("Lời mời đã được gửi trước đó và đang chờ xác nhận");
            }
        });

        try {
            authServiceClient.inviteUser(new InviteUserReq(req.getEmail()));
        } catch (Exception e) {
            log.error("Failed to call auth-service to invite user", e);
            throw new RuntimeException("Dịch vụ định danh tạm thời không khả dụng, vui lòng thử lại sau", e);
        }

        TenantMember member = TenantMember.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .email(req.getEmail())
                .roleId(req.getRoleId())
                .status(TenantMemberStatus.PENDING)
                .invitedAt(LocalDateTime.now())
                .build();

        member = tenantMemberRepository.save(member);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("email", req.getEmail());
        payload.put("roleId", req.getRoleId());
        payload.put("invitedAt", member.getInvitedAt());

        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("TenantMember")
                    .aggregateId(member.getId())
                    .type("tenant.member_invited")
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }

        return TenantMemberRes.builder()
                .id(member.getId())
                .tenantId(member.getTenantId())
                .email(member.getEmail())
                .roleId(member.getRoleId())
                .status(member.getStatus())
                .message("Lời mời đã được gửi thành công")
                .invitedAt(member.getInvitedAt())
                .build();
    }
}
