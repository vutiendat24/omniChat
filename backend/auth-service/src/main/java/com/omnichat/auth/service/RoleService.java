package com.omnichat.auth.service;

import com.omnichat.auth.dto.RoleReq;
import com.omnichat.auth.dto.RoleRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleService {
    Page<RoleRes> getRoles(String name, Pageable pageable);
    RoleRes createRole(RoleReq request);
    RoleRes updateRole(Long roleId, RoleReq request);
    void deleteRole(Long roleId);
    
    void assignPermissionsToRole(Long roleId, com.omnichat.auth.dto.AssignPermissionsReq request);
    java.util.List<com.omnichat.auth.dto.PermissionRes> getAllPermissions();
}
