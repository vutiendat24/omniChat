package com.omnichat.tenant.repository;

import com.omnichat.tenant.domain.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, String> {
    long countByTenantId(String tenantId);
    boolean existsByTenantIdAndNameIgnoreCase(String tenantId, String name);
}
