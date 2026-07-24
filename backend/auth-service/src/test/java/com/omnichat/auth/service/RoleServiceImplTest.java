package com.omnichat.auth.service;

import com.omnichat.auth.domain.entity.Role;
import com.omnichat.auth.dto.RoleReq;
import com.omnichat.auth.dto.RoleRes;
import com.omnichat.auth.exception.RoleException;
import com.omnichat.auth.repository.RoleRepository;
import com.omnichat.auth.repository.UserRepository;
import com.omnichat.auth.service.implement.RoleServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role systemRole;
    private Role customRole;
    private RoleReq roleReq;

    @BeforeEach
    void setUp() {
        systemRole = new Role(1L, "SUPER_ADMIN", "Admin role", true);
        customRole = new Role(2L, "AGENT", "Agent role", false);

        roleReq = new RoleReq();
        roleReq.setRoleName("AGENT");
        roleReq.setDescription("Updated description");
    }

    @Test
    void createRole_Success() {
        RoleReq newRoleReq = new RoleReq();
        newRoleReq.setRoleName("NEW_ROLE");
        newRoleReq.setDescription("New role desc");

        when(roleRepository.findByName("NEW_ROLE")).thenReturn(Optional.empty());
        
        Role savedRole = new Role(3L, "NEW_ROLE", "New role desc", false);
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        RoleRes res = roleService.createRole(newRoleReq);

        assertEquals("NEW_ROLE", res.getName());
        assertEquals("New role desc", res.getDescription());
        assertFalse(res.isSystem());
    }

    @Test
    void createRole_AlreadyExists_ThrowsException() {
        when(roleRepository.findByName("AGENT")).thenReturn(Optional.of(customRole));
        
        RoleException exception = assertThrows(RoleException.class, () -> roleService.createRole(roleReq));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Tên vai trò đã tồn tại", exception.getMessage());
    }

    @Test
    void updateRole_Success() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(customRole));
        
        // Simulating the save
        Role updatedRole = new Role(2L, "AGENT", "Updated description", false);
        when(roleRepository.save(any(Role.class))).thenReturn(updatedRole);

        RoleRes res = roleService.updateRole(2L, roleReq);

        assertEquals("AGENT", res.getName());
        assertEquals("Updated description", res.getDescription());
    }

    @Test
    void updateRole_SystemRole_ThrowsException() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(systemRole));

        RoleException exception = assertThrows(RoleException.class, () -> roleService.updateRole(1L, roleReq));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Không thể sửa vai trò mặc định của hệ thống", exception.getMessage());
    }

    @Test
    void deleteRole_Success() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(customRole));
        when(userRepository.existsByRolesId(2L)).thenReturn(false);

        roleService.deleteRole(2L);

        verify(roleRepository, times(1)).delete(customRole);
    }

    @Test
    void deleteRole_SystemRole_ThrowsException() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(systemRole));

        RoleException exception = assertThrows(RoleException.class, () -> roleService.deleteRole(1L));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Không thể xóa vai trò mặc định của hệ thống", exception.getMessage());
    }

    @Test
    void deleteRole_InUse_ThrowsException() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(customRole));
        when(userRepository.existsByRolesId(2L)).thenReturn(true);

        RoleException exception = assertThrows(RoleException.class, () -> roleService.deleteRole(2L));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Không thể xóa vai trò đang được gán cho người dùng", exception.getMessage());
    }

    @Test
    void getRoles_WithSearchName() {
        Page<Role> page = new PageImpl<>(List.of(customRole));
        when(roleRepository.findByNameContainingIgnoreCase("AGE", PageRequest.of(0, 10))).thenReturn(page);

        Page<RoleRes> res = roleService.getRoles(" AGE ", PageRequest.of(0, 10));

        assertEquals(1, res.getTotalElements());
        assertEquals("AGENT", res.getContent().get(0).getName());
    }
}
