package com.omnichat.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.tenant.domain.entity.OutboxEvent;
import com.omnichat.tenant.domain.entity.Plan;
import com.omnichat.tenant.domain.entity.Team;
import com.omnichat.tenant.domain.entity.Tenant;
import com.omnichat.tenant.dto.CreateTeamReq;
import com.omnichat.tenant.dto.TeamRes;
import com.omnichat.tenant.dto.UpdateTeamReq;
import com.omnichat.tenant.exception.DuplicateResourceException;
import com.omnichat.tenant.exception.QuotaExceededException;
import com.omnichat.tenant.exception.ResourceNotFoundException;
import com.omnichat.tenant.repository.OutboxEventRepository;
import com.omnichat.tenant.repository.PlanRepository;
import com.omnichat.tenant.repository.TeamRepository;
import com.omnichat.tenant.repository.TenantRepository;
import com.omnichat.tenant.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TeamService teamService;

    private String tenantId;
    private Tenant tenant;
    private Plan basicPlan;
    private Plan proPlan;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID().toString();
        tenant = Tenant.builder()
                .id(tenantId)
                .name("My Shop")
                .planId("Basic")
                .build();
                
        basicPlan = Plan.builder()
                .id("Basic")
                .maxTeams(3)
                .build();
                
        proPlan = Plan.builder()
                .id("Pro")
                .maxTeams(-1)
                .build();
    }

    @Test
    void testCreateTeam_Success() throws JsonProcessingException {
        CreateTeamReq req = CreateTeamReq.builder()
                .teamName("Sales Team")
                .description("Handles sales")
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(planRepository.findById("Basic")).thenReturn(Optional.of(basicPlan));
        when(teamRepository.countByTenantId(tenantId)).thenReturn(2L); // 2 < 3, ok
        when(teamRepository.existsByTenantIdAndNameIgnoreCase(tenantId, "Sales Team")).thenReturn(false);
        
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID().toString());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });
        
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{}");

        TeamRes res = teamService.createTeam(tenantId, req);
        
        assertNotNull(res);
        assertEquals("Sales Team", res.getTeamName());
        
        verify(teamRepository, times(1)).save(any(Team.class));
        verify(outboxEventRepository, times(1)).save(argThat(event -> 
            event.getAggregateType().equals("Team") && event.getType().equals("team.created")
        ));
    }

    @Test
    void testCreateTeam_DuplicateName_ThrowsException() {
        CreateTeamReq req = CreateTeamReq.builder()
                .teamName("Sales Team")
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(planRepository.findById("Basic")).thenReturn(Optional.of(basicPlan));
        when(teamRepository.countByTenantId(tenantId)).thenReturn(1L);
        when(teamRepository.existsByTenantIdAndNameIgnoreCase(tenantId, "Sales Team")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> teamService.createTeam(tenantId, req));
        
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void testCreateTeam_QuotaExceeded_ThrowsException() {
        CreateTeamReq req = CreateTeamReq.builder()
                .teamName("Sales Team")
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(planRepository.findById("Basic")).thenReturn(Optional.of(basicPlan));
        when(teamRepository.countByTenantId(tenantId)).thenReturn(3L); // 3 >= 3, fail

        assertThrows(QuotaExceededException.class, () -> teamService.createTeam(tenantId, req));
        
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void testCreateTeam_ProPlan_UnlimitedQuota_Success() throws JsonProcessingException {
        tenant.setPlanId("Pro");
        CreateTeamReq req = CreateTeamReq.builder()
                .teamName("Sales Team")
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(planRepository.findById("Pro")).thenReturn(Optional.of(proPlan));
        // Since maxTeams = -1, it doesn't call countByTenantId
        when(teamRepository.existsByTenantIdAndNameIgnoreCase(tenantId, "Sales Team")).thenReturn(false);
        
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID().toString());
            return saved;
        });
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{}");

        TeamRes res = teamService.createTeam(tenantId, req);
        assertNotNull(res);
        verify(teamRepository, times(1)).save(any(Team.class));
    }

    @Test
    void testUpdateTeam_Success() throws JsonProcessingException {
        String teamId = UUID.randomUUID().toString();
        Team existingTeam = Team.builder()
                .id(teamId)
                .tenantId(tenantId)
                .name("Old Name")
                .description("Old Desc")
                .version(1L)
                .build();

        UpdateTeamReq req = UpdateTeamReq.builder()
                .teamName("New Name")
                .description("New Desc")
                .version(1L)
                .build();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existingTeam));
        // Same tenant ID check
        when(teamRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, "New Name", teamId)).thenReturn(false);
        
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{}");

        TeamRes res = teamService.updateTeam(tenantId, teamId, req);

        assertNotNull(res);
        assertEquals("New Name", res.getTeamName());
        assertEquals("New Desc", res.getDescription());
        
        verify(teamRepository, times(1)).save(any(Team.class));
        verify(outboxEventRepository, times(1)).save(argThat(event -> 
            event.getAggregateType().equals("Team") && event.getType().equals("team.updated")
        ));
    }

    @Test
    void testUpdateTeam_DuplicateName_ThrowsException() {
        String teamId = UUID.randomUUID().toString();
        Team existingTeam = Team.builder()
                .id(teamId)
                .tenantId(tenantId)
                .name("Old Name")
                .build();

        UpdateTeamReq req = UpdateTeamReq.builder()
                .teamName("Existing Name")
                .version(1L)
                .build();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existingTeam));
        when(teamRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, "Existing Name", teamId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> teamService.updateTeam(tenantId, teamId, req));
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void testUpdateTeam_NotFound_ThrowsException() {
        String teamId = UUID.randomUUID().toString();
        UpdateTeamReq req = UpdateTeamReq.builder()
                .teamName("New Name")
                .version(1L)
                .build();

        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teamService.updateTeam(tenantId, teamId, req));
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void testUpdateTeam_WrongTenantId_ThrowsException() {
        String teamId = UUID.randomUUID().toString();
        Team existingTeam = Team.builder()
                .id(teamId)
                .tenantId("another-tenant-id")
                .name("Old Name")
                .build();

        UpdateTeamReq req = UpdateTeamReq.builder()
                .teamName("New Name")
                .version(1L)
                .build();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existingTeam));

        assertThrows(ResourceNotFoundException.class, () -> teamService.updateTeam(tenantId, teamId, req));
        verify(teamRepository, never()).save(any(Team.class));
    }
}
