package com.omnichat.auth.repository;

import com.omnichat.auth.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    org.springframework.data.domain.Page<Role> findByNameContainingIgnoreCase(String name, org.springframework.data.domain.Pageable pageable);
}
