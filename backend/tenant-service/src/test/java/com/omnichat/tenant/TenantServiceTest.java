package com.omnichat.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.tenant.client.AuthServiceClient;
import com.omnichat.tenant.domain.entity.Tenant;
import com.omnichat.tenant.dto.CreateTenantReq;
import com.omnichat.tenant.dto.TenantRes;
import com.omnichat.tenant.exception.DuplicateResourceException;
import com.omnichat.tenant.repository.OutboxEventRepository;
import com.omnichat.tenant.repository.TenantRepository;
import com.omnichat.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TenantService tenantService;

    private CreateTenantReq validReq;

    @BeforeEach
    void setUp() {
        validReq = CreateTenantReq.builder()
                .tenantName("Happy Shop")
                .slug("happy-shop")
                .ownerEmail("owner@happyshop.com")
                .ownerName("John Doe")
                .planId("Trial")
                .build();
    }

    @Test
    void createTenant_Success() throws Exception {
        when(tenantRepository.existsBySlug(validReq.getSlug())).thenReturn(false);
        when(authServiceClient.createOwnerAccount(any())).thenReturn(ResponseEntity.ok().build());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        TenantRes res = tenantService.createTenant(validReq);

        assertNotNull(res);
        assertNotNull(res.getTenantId());
        verify(tenantRepository, times(1)).save(any(Tenant.class));
        verify(outboxEventRepository, times(1)).save(any());
        verify(authServiceClient, times(1)).createOwnerAccount(any());
    }

    @Test
    void createTenant_DuplicateSlug_ThrowsConflict() {
        when(tenantRepository.existsBySlug(validReq.getSlug())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            tenantService.createTenant(validReq);
        });

        verify(tenantRepository, never()).save(any());
        verify(authServiceClient, never()).createOwnerAccount(any());
    }

    @Test
    void createTenant_AuthServiceFails_ThrowsException() {
        when(tenantRepository.existsBySlug(validReq.getSlug())).thenReturn(false);
        when(authServiceClient.createOwnerAccount(any())).thenThrow(new RuntimeException("Auth service down"));

        assertThrows(RuntimeException.class, () -> {
            tenantService.createTenant(validReq);
        });

        verify(tenantRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }
}
