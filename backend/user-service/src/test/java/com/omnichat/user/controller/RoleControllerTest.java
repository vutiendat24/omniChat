package com.omnichat.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.user.domain.entity.*;
import com.omnichat.user.dto.RolePermissionReq;
import com.omnichat.user.dto.RoleReq;
import com.omnichat.user.repository.PermissionRepository;
import com.omnichat.user.repository.RoleRepository;
import com.omnichat.user.repository.UserRepository;
import com.omnichat.user.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private StringRedisTemplate redisTemplate;
    
    @MockBean
    private ValueOperations<String, String> valueOperations;

    private User adminUser;
    private User managerUser;
    private User ownerUser;
    private Role ownerRole;
    private Role adminRole;
    private Role managerRole;
    private WorkspaceMember adminMember;
    private WorkspaceMember managerMember;
    private WorkspaceMember ownerMember;
    
    private Permission deleteUserPerm;
    private Permission viewReportPerm;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        jdbcTemplate.execute("DELETE FROM role_permissions");
        jdbcTemplate.execute("DELETE FROM permissions");
        jdbcTemplate.execute("DELETE FROM workspace_members");
        jdbcTemplate.execute("DELETE FROM roles");
        jdbcTemplate.execute("DELETE FROM users");

        deleteUserPerm = permissionRepository.save(Permission.builder().name("USER_DELETE").description("Delete user").build());
        viewReportPerm = permissionRepository.save(Permission.builder().name("REPORT_VIEW").description("View report").build());

        ownerRole = roleRepository.save(Role.builder().workspaceId(1L).name("Owner").level(100).isSystem(true).permissions(Set.of(deleteUserPerm, viewReportPerm)).build());
        adminRole = roleRepository.save(Role.builder().workspaceId(1L).name("Admin").level(80).isSystem(true).permissions(Set.of(viewReportPerm)).build());
        managerRole = roleRepository.save(Role.builder().workspaceId(1L).name("Manager").level(60).isSystem(true).build());

        ownerUser = userRepository.save(User.builder().email("owner@omnichat.com").password("Pass@123").fullName("Owner").build());
        adminUser = userRepository.save(User.builder().email("admin@omnichat.com").password("Pass@123").fullName("Admin").build());
        managerUser = userRepository.save(User.builder().email("manager@omnichat.com").password("Pass@123").fullName("Manager").build());

        ownerMember = workspaceMemberRepository.save(WorkspaceMember.builder().workspaceId(1L).user(ownerUser).role(ownerRole).build());
        adminMember = workspaceMemberRepository.save(WorkspaceMember.builder().workspaceId(1L).user(adminUser).role(adminRole).build());
        managerMember = workspaceMemberRepository.save(WorkspaceMember.builder().workspaceId(1L).user(managerUser).role(managerRole).build());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldCreateRoleSuccessfully() throws Exception {
        RoleReq req = new RoleReq("Editor", "Editor role", 50);
        mockMvc.perform(post("/api/v1/workspaces/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldRejectCreateRoleIfLevelGreaterOrEqual() throws Exception {
        RoleReq req = new RoleReq("SuperAdmin", "Super Admin", 80);
        mockMvc.perform(post("/api/v1/workspaces/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldRejectCreateRoleIfNameExists() throws Exception {
        RoleReq req = new RoleReq("Manager", "Manager role", 50);
        mockMvc.perform(post("/api/v1/workspaces/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldRejectDeleteSystemRole() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/1/roles/" + managerRole.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldRejectDeleteRoleWithMembers() throws Exception {
        Role customRole = roleRepository.save(Role.builder().workspaceId(1L).name("Marketing").level(50).isSystem(false).build());
        
        User user1 = userRepository.save(User.builder().email("user1@omnichat.com").password("Pass@123").fullName("User 1").build());
        workspaceMemberRepository.save(WorkspaceMember.builder().workspaceId(1L).user(user1).role(customRole).build());

        mockMvc.perform(delete("/api/v1/workspaces/1/roles/" + customRole.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldDeleteRoleSuccessfullyIfNoMembers() throws Exception {
        Role customRole = roleRepository.save(Role.builder().workspaceId(1L).name("Sales").level(50).isSystem(false).build());

        mockMvc.perform(delete("/api/v1/workspaces/1/roles/" + customRole.getId()))
                .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldUpdateRolePermissionsSuccessfully() throws Exception {
        RolePermissionReq req = new RolePermissionReq(List.of(viewReportPerm.getId()));
        
        mockMvc.perform(put("/api/v1/workspaces/1/roles/" + managerRole.getId() + "/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldRejectUpdateRolePermissionsIfActorDoesNotHaveIt() throws Exception {
        RolePermissionReq req = new RolePermissionReq(List.of(deleteUserPerm.getId()));
        
        mockMvc.perform(put("/api/v1/workspaces/1/roles/" + managerRole.getId() + "/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@omnichat.com")
    void ownerCanUpdateAnyRolePermissions() throws Exception {
        RolePermissionReq req = new RolePermissionReq(List.of(deleteUserPerm.getId(), viewReportPerm.getId()));
        
        mockMvc.perform(put("/api/v1/workspaces/1/roles/" + adminRole.getId() + "/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
