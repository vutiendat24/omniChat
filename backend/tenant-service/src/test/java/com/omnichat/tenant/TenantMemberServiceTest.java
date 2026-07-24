package com.omnichat.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.tenant.client.AuthServiceClient;
import com.omnichat.tenant.domain.entity.*;
import com.omnichat.tenant.dto.InviteMemberReq;
import com.omnichat.tenant.dto.TenantMemberRes;
import com.omnichat.tenant.exception.DuplicateResourceException;
import com.omnichat.tenant.exception.QuotaExceededException;
import com.omnichat.tenant.exception.ResourceNotFoundException;
import com.omnichat.tenant.repository.OutboxEventRepository;
import com.omnichat.tenant.repository.PlanRepository;
import com.omnichat.tenant.repository.TenantMemberRepository;
import com.omnichat.tenant.repository.TenantRepository;
import com.omnichat.tenant.repository.UserTeamRepository;
import com.omnichat.tenant.service.TenantMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TenantMemberServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantMemberRepository tenantMemberRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private UserTeamRepository userTeamRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TenantMemberService tenantMemberService;

    private String tenantId;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID().toString();
        
        Plan plan = Plan.builder()
                .id("Basic")
                .maxUsers(5)
                .build();

        tenant = Tenant.builder()
                .id(tenantId)
                .ownerEmail("owner@example.com")
                .planId("Basic")
                .build();
    }

    @Test
    void testInviteMember_Success() throws JsonProcessingException {
        InviteMemberReq req = new InviteMemberReq("agent@example.com", "ROLE_AGENT");
        Plan plan = Plan.builder().id("Basic").maxUsers(5).build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(planRepository.findById("Basic")).thenReturn(Optional.of(plan));
        when(tenantMemberRepository.countByTenantIdAndStatusIn(eq(tenantId), anyList())).thenReturn(2L);
        when(tenantMemberRepository.findByTenantIdAndEmail(tenantId, req.getEmail())).thenReturn(Optional.empty());
        when(authServiceClient.inviteUser(any())).thenReturn(ResponseEntity.ok().build());
        when(tenantMemberRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{}");

        TenantMemberRes res = tenantMemberService.inviteMember(tenantId, req);

        assertNotNull(res);
        assertEquals("agent@example.com", res.getEmail());
        assertEquals("ROLE_AGENT", res.getRoleId());
        assertEquals(TenantMemberStatus.PENDING, res.getStatus());
        assertEquals("Lời mời đã được gửi thành công", res.getMessage());

        verify(authServiceClient, times(1)).inviteUser(any());
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void testInviteMember_Self_ThrowsException() {
        InviteMemberReq req = new InviteMemberReq("owner@example.com", "ROLE_AGENT");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tenantMemberService.inviteMember(tenantId, req));
        assertEquals("Bạn không thể tự mời chính mình vào không gian làm việc này", ex.getMessage());
    }

    @Test
    void testInviteMember_QuotaExceeded_ThrowsException() {
        InviteMemberReq req = new InviteMemberReq("agent@example.com", "ROLE_AGENT");
        Plan plan = Plan.builder().id("Basic").maxUsers(5).build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(planRepository.findById("Basic")).thenReturn(Optional.of(plan));
        when(tenantMemberRepository.countByTenantIdAndStatusIn(eq(tenantId), anyList())).thenReturn(4L); // +1 = 5, quota is 5. So it will fail because currentMembers(4) + 1 >= 5. Wait, if max=5, and we have 4, then current(4) + 1 = 5 >= 5 -> fails.

        assertThrows(QuotaExceededException.class, () -> tenantMemberService.inviteMember(tenantId, req));
    }

    @Test
    void testInviteMember_DuplicatePending_ThrowsException() {
        InviteMemberReq req = new InviteMemberReq("agent@example.com", "ROLE_AGENT");
        Plan plan = Plan.builder().id("Basic").maxUsers(5).build();

        TenantMember existing = TenantMember.builder().status(TenantMemberStatus.PENDING).build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(planRepository.findById("Basic")).thenReturn(Optional.of(plan));
        when(tenantMemberRepository.countByTenantIdAndStatusIn(eq(tenantId), anyList())).thenReturn(2L);
        when(tenantMemberRepository.findByTenantIdAndEmail(tenantId, req.getEmail())).thenReturn(Optional.of(existing));

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> tenantMemberService.inviteMember(tenantId, req));
        assertEquals("Lời mời đã được gửi trước đó và đang chờ xác nhận", ex.getMessage());
    }

    @Test
    void testRemoveMember_Success() throws JsonProcessingException {
        String userId = UUID.randomUUID().toString();
        String currentUserId = UUID.randomUUID().toString();
        TenantMember member = TenantMember.builder().id(userId).email("user@example.com").roleId("ROLE_AGENT").tenantId(tenantId).build();

        when(tenantMemberRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(member));
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{}");

        tenantMemberService.removeMember(currentUserId, tenantId, userId);

        verify(userTeamRepository, times(1)).deleteByUserId(userId);
        verify(tenantMemberRepository, times(1)).delete(member);
        verify(authServiceClient, times(1)).revokeTokensByEmails(any());
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void testRemoveMember_SelfRemove_ThrowsException() {
        String userId = "self-id";
        
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tenantMemberService.removeMember(userId, tenantId, userId));
        assertEquals("Bạn không thể tự xóa chính mình. Vui lòng sử dụng tính năng Rời khỏi cửa hàng (Leave Workspace)", ex.getMessage());
        
        verify(tenantMemberRepository, never()).delete(any());
    }

    @Test
    void testRemoveMember_LastOwner_ThrowsException() {
        String userId = UUID.randomUUID().toString();
        String currentUserId = UUID.randomUUID().toString();
        TenantMember member = TenantMember.builder().id(userId).email("owner@example.com").roleId("TENANT_OWNER").tenantId(tenantId).build();

        when(tenantMemberRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(member));
        when(tenantMemberRepository.countByTenantIdAndRoleId(tenantId, "TENANT_OWNER")).thenReturn(1L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tenantMemberService.removeMember(currentUserId, tenantId, userId));
        assertEquals("Không thể xóa chủ sở hữu duy nhất của cửa hàng", ex.getMessage());
        
        verify(tenantMemberRepository, never()).delete(any());
    }

    @Test
    void testRemoveMember_AuthServiceFails_FallbackSuccess() throws JsonProcessingException {
        String userId = UUID.randomUUID().toString();
        String currentUserId = UUID.randomUUID().toString();
        TenantMember member = TenantMember.builder().id(userId).email("user@example.com").roleId("ROLE_AGENT").tenantId(tenantId).build();

        when(tenantMemberRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(member));
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{}");
        doThrow(new RuntimeException("Auth service down")).when(authServiceClient).revokeTokensByEmails(any());

        assertDoesNotThrow(() -> tenantMemberService.removeMember(currentUserId, tenantId, userId));

        verify(userTeamRepository, times(1)).deleteByUserId(userId);
        verify(tenantMemberRepository, times(1)).delete(member);
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }
}
