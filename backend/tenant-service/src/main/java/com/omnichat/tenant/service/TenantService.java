package com.omnichat.tenant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.tenant.client.AuthServiceClient;
import com.omnichat.tenant.domain.entity.OutboxEvent;
import com.omnichat.tenant.domain.entity.Tenant;
import com.omnichat.tenant.dto.CreateOwnerReq;
import com.omnichat.tenant.dto.CreateTenantReq;
import com.omnichat.tenant.dto.UpdateTenantReq;
import com.omnichat.tenant.dto.TenantRes;
import com.omnichat.tenant.exception.DuplicateResourceException;
import com.omnichat.tenant.exception.ResourceNotFoundException;
import com.omnichat.tenant.repository.OutboxEventRepository;
import com.omnichat.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AuthServiceClient authServiceClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TenantRes createTenant(CreateTenantReq req) {
        if (tenantRepository.existsBySlug(req.getSlug())) {
            throw new DuplicateResourceException("Mã định danh (slug) đã được sử dụng");
        }

        String tempTenantId = UUID.randomUUID().toString();
        CreateOwnerReq ownerReq = CreateOwnerReq.builder()
                .email(req.getOwnerEmail())
                .fullName(req.getOwnerName())
                .tenantId(tempTenantId)
                .build();
        
        try {
            authServiceClient.createOwnerAccount(ownerReq);
        } catch (Exception e) {
            log.error("Failed to call auth-service to create owner account", e);
            throw new RuntimeException("Identity service is unavailable", e);
        }

        Tenant tenant = Tenant.builder()
                .id(tempTenantId)
                .name(req.getTenantName())
                .slug(req.getSlug())
                .ownerEmail(req.getOwnerEmail())
                .planId(req.getPlanId() != null ? req.getPlanId() : "Trial")
                .build();
        
        tenant = tenantRepository.save(tenant);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenant.getId());
        payload.put("slug", tenant.getSlug());
        payload.put("planId", tenant.getPlanId());
        payload.put("createdAt", tenant.getCreatedAt());

        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Tenant")
                    .aggregateId(tenant.getId())
                    .type("tenant.created")
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }

        return TenantRes.builder()
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .slug(tenant.getSlug())
                .status(tenant.getStatus())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }

    @Transactional
    public TenantRes updateTenant(String tenantId, UpdateTenantReq request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (!tenant.getVersion().equals(request.getVersion())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Tenant.class, tenant.getId());
        }

        if (request.getTenantName() != null && !request.getTenantName().trim().isEmpty()) {
            tenant.setName(request.getTenantName());
        }
        if (request.getLogoUrl() != null) {
            tenant.setLogoUrl(request.getLogoUrl());
        }
        if (request.getIndustry() != null) {
            tenant.setIndustry(request.getIndustry());
        }
        if (request.getContactEmail() != null) {
            tenant.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            tenant.setContactPhone(request.getContactPhone());
        }
        if (request.getAddress() != null) {
            tenant.setAddress(request.getAddress());
        }

        Tenant savedTenant = tenantRepository.save(tenant);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", savedTenant.getId());
        payload.put("tenantName", savedTenant.getName());
        payload.put("updatedFields", request);
        payload.put("updatedAt", savedTenant.getUpdatedAt());

        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Tenant")
                    .aggregateId(savedTenant.getId())
                    .type("tenant.updated")
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }

        return TenantRes.builder()
                .tenantId(savedTenant.getId())
                .tenantName(savedTenant.getName())
                .slug(savedTenant.getSlug())
                .logoUrl(savedTenant.getLogoUrl())
                .industry(savedTenant.getIndustry())
                .contactEmail(savedTenant.getContactEmail())
                .contactPhone(savedTenant.getContactPhone())
                .address(savedTenant.getAddress())
                .status(savedTenant.getStatus())
                .createdAt(savedTenant.getCreatedAt())
                .updatedAt(savedTenant.getUpdatedAt())
                .build();
    }
}
