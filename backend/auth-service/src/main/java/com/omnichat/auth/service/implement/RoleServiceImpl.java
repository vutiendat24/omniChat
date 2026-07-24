package com.omnichat.auth.service.implement;

import com.omnichat.auth.domain.entity.Role;
import com.omnichat.auth.dto.RoleReq;
import com.omnichat.auth.dto.RoleRes;
import com.omnichat.auth.exception.RoleException;
import com.omnichat.auth.repository.RoleRepository;
import com.omnichat.auth.repository.UserRepository;
import com.omnichat.auth.service.RoleService;

import com.omnichat.auth.dto.AssignPermissionsReq;
import com.omnichat.auth.dto.PermissionRes;
import com.omnichat.auth.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public Page<RoleRes> getRoles(String name, Pageable pageable) {
        Page<Role> roles;
        if (name != null && !name.trim().isEmpty()) {
            roles = roleRepository.findByNameContainingIgnoreCase(name.trim(), pageable);
        } else {
            roles = roleRepository.findAll(pageable);
        }
        return roles.map(this::mapToRoleRes);
    }

    @Override
    @Transactional
    public RoleRes createRole(RoleReq request) {
        if (roleRepository.findByName(request.getRoleName()).isPresent()) {
            throw new RoleException(HttpStatus.CONFLICT, "Tên vai trò đã tồn tại");
        }

        Role role = new Role();
        role.setName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setSystem(false);

        role = roleRepository.save(role);
        return mapToRoleRes(role);
    }

    @Override
    @Transactional
    public RoleRes updateRole(Long roleId, RoleReq request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleException(HttpStatus.NOT_FOUND, "Không tìm thấy vai trò"));

        if (role.isSystem()) {
            throw new RoleException(HttpStatus.FORBIDDEN, "Không thể sửa vai trò mặc định của hệ thống");
        }

        if (!role.getName().equals(request.getRoleName()) && 
            roleRepository.findByName(request.getRoleName()).isPresent()) {
            throw new RoleException(HttpStatus.CONFLICT, "Tên vai trò đã tồn tại");
        }

        role.setName(request.getRoleName());
        role.setDescription(request.getDescription());
        role = roleRepository.save(role);
        return mapToRoleRes(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleException(HttpStatus.NOT_FOUND, "Không tìm thấy vai trò"));

        if (role.isSystem()) {
            throw new RoleException(HttpStatus.FORBIDDEN, "Không thể xóa vai trò mặc định của hệ thống");
        }

        if (userRepository.existsByRolesId(roleId)) {
            throw new RoleException(HttpStatus.CONFLICT, "Không thể xóa vai trò đang được gán cho người dùng");
        }

        roleRepository.delete(role);
    }

    @Override
    public List<PermissionRes> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> PermissionRes.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignPermissionsToRole(Long roleId, AssignPermissionsReq request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleException(HttpStatus.NOT_FOUND, "Không tìm thấy vai trò"));

        if (role.getName().equals("SUPER_ADMIN")) {
            throw new RoleException(HttpStatus.FORBIDDEN, "Không thể thay đổi quyền của vai trò hệ thống");
        }

        List<com.omnichat.auth.domain.entity.Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
        role.setPermissions(new HashSet<>(permissions));
        roleRepository.save(role);
    }

    private RoleRes mapToRoleRes(Role role) {
        return RoleRes.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystem(role.isSystem())
                .build();
    }
}
