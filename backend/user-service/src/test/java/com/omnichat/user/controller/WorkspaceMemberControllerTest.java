package com.omnichat.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichat.user.domain.entity.*;
import com.omnichat.user.dto.InviteMemberReq;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WorkspaceMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private User adminUser;
    private User managerUser;
    private User ownerUser;
    private Role ownerRole;
    private Role adminRole;
    private Role managerRole;
    private WorkspaceMember adminMember;
    private WorkspaceMember managerMember;
    private WorkspaceMember ownerMember;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM workspace_members");
        jdbcTemplate.execute("DELETE FROM roles");
        jdbcTemplate.execute("DELETE FROM users");

        ownerRole = roleRepository.save(Role.builder().name("Owner").level(100).build());
        adminRole = roleRepository.save(Role.builder().name("Admin").level(80).build());
        managerRole = roleRepository.save(Role.builder().name("Manager").level(60).build());

        ownerUser = userRepository.save(User.builder().email("owner@omnichat.com").password("Pass@123").fullName("Owner").build());
        adminUser = userRepository.save(User.builder().email("admin@omnichat.com").password("Pass@123").fullName("Admin").build());
        managerUser = userRepository.save(User.builder().email("manager@omnichat.com").password("Pass@123").fullName("Manager").build());

        ownerMember = workspaceMemberRepository.save(WorkspaceMember.builder().workspaceId(1L).user(ownerUser).role(ownerRole).build());
        adminMember = workspaceMemberRepository.save(WorkspaceMember.builder().workspaceId(1L).user(adminUser).role(adminRole).build());
        managerMember = workspaceMemberRepository.save(WorkspaceMember.builder().workspaceId(1L).user(managerUser).role(managerRole).build());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldDeleteManagerSuccessfullyByAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/1/members/" + managerUser.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldRejectDeleteAdminByAdmin() throws Exception {
        User anotherAdmin = userRepository.save(User.builder().email("admin2@omnichat.com").password("Pass@123").fullName("Admin 2").build());
        workspaceMemberRepository.save(WorkspaceMember.builder().workspaceId(1L).user(anotherAdmin).role(adminRole).build());

        mockMvc.perform(delete("/api/v1/workspaces/1/members/" + anotherAdmin.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldRejectDeleteOwnerByAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/1/members/" + ownerUser.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldRejectDeleteSelf() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/1/members/" + adminUser.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@omnichat.com")
    void shouldRejectInviteAdminByManager() throws Exception {
        InviteMemberReq req = new InviteMemberReq("new@omnichat.com", adminRole.getId());
        mockMvc.perform(post("/api/v1/workspaces/1/members/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldRejectInviteOwnerByAdmin() throws Exception {
        InviteMemberReq req = new InviteMemberReq("new@omnichat.com", ownerRole.getId());
        mockMvc.perform(post("/api/v1/workspaces/1/members/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@omnichat.com")
    void shouldInviteManagerSuccessfullyByAdmin() throws Exception {
        InviteMemberReq req = new InviteMemberReq("new@omnichat.com", managerRole.getId());
        mockMvc.perform(post("/api/v1/workspaces/1/members/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
