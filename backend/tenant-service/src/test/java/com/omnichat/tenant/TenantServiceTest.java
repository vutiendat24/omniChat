package com.omnichat.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.tenant.client.AuthServiceClient;
import com.omnichat.tenant.domain.entity.Tenant;
import com.omnichat.tenant.dto.CreateTenantReq;
import com.omnichat.tenant.dto.UpdateTenantReq;
import com.omnichat.tenant.dto.UpdateTenantStatusReq;
import com.omnichat.tenant.dto.TenantRes;
import com.omnichat.tenant.domain.entity.TenantStatus;
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
import java.util.UUID;
import java.util.List;

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

    @Test
    void testUpdateTenant_Success() {
        String tenantId = UUID.randomUUID().toString();
        Tenant existingTenant = Tenant.builder()
                .id(tenantId)
                .name("Old Name")
                .slug("old-slug")
                .ownerEmail("owner@test.com")
                .status(com.omnichat.tenant.domain.entity.TenantStatus.ACTIVE)
                .version(1L)
                .build();

        UpdateTenantReq req = UpdateTenantReq.builder()
                .tenantName("New Name")
                .logoUrl("https://example.com/logo.png")
                .version(1L)
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existingTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantRes res = tenantService.updateTenant(tenantId, req);

        assertNotNull(res);
        assertEquals("New Name", res.getTenantName());
        assertEquals("old-slug", res.getSlug()); // Slug should not change
        
        verify(tenantRepository, times(1)).save(any(Tenant.class));
        verify(outboxEventRepository, times(1)).save(argThat(event -> 
            event.getAggregateType().equals("Tenant") && event.getType().equals("tenant.updated")
        ));
    }

    @Test
    void testUpdateTenant_NotFound() {
        String tenantId = UUID.randomUUID().toString();
        UpdateTenantReq req = UpdateTenantReq.builder().tenantName("New Name").version(1L).build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(com.omnichat.tenant.exception.ResourceNotFoundException.class, () -> {
            tenantService.updateTenant(tenantId, req);
        });
        
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void testUpdateTenant_ConcurrencyConflict() {
        String tenantId = UUID.randomUUID().toString();
        Tenant existingTenant = Tenant.builder()
                .id(tenantId)
                .name("Old Name")
                .version(2L) // Current DB version is 2
                .build();

        UpdateTenantReq req = UpdateTenantReq.builder()
                .tenantName("New Name")
                .version(1L) // Client sent old version 1
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existingTenant));

        assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class, () -> {
            tenantService.updateTenant(tenantId, req);
        });

        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void testUpdateTenantStatus_Success_ToInactive() {
        String tenantId = UUID.randomUUID().toString();
        Tenant existingTenant = Tenant.builder()
                .id(tenantId)
                .name("Test")
                .ownerEmail("owner@test.com")
                .status(TenantStatus.ACTIVE)
                .build();

        UpdateTenantStatusReq req = UpdateTenantStatusReq.builder()
                .status(TenantStatus.INACTIVE)
                .reason("Expired")
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existingTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantRes res = tenantService.updateTenantStatus(tenantId, req);

        assertNotNull(res);
        assertEquals(TenantStatus.INACTIVE, res.getStatus());
        
        verify(authServiceClient, times(1)).revokeTokensByEmails(argThat(r -> r.getEmails().contains("owner@test.com")));
        verify(outboxEventRepository, times(1)).save(argThat(event -> 
            event.getAggregateType().equals("Tenant") && event.getType().equals("tenant.status_changed")
        ));
    }

    @Test
    void testUpdateTenantStatus_SameStatus_ThrowsException() {
        String tenantId = UUID.randomUUID().toString();
        Tenant existingTenant = Tenant.builder()
                .id(tenantId)
                .status(TenantStatus.ACTIVE)
                .build();

        UpdateTenantStatusReq req = UpdateTenantStatusReq.builder()
                .status(TenantStatus.ACTIVE)
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existingTenant));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            tenantService.updateTenantStatus(tenantId, req);
        });

        assertTrue(ex.getMessage().contains("Cannot change to the same status"));
        verify(tenantRepository, never()).save(any(Tenant.class));
    }
}
