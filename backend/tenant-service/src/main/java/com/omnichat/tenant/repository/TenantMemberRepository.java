package com.omnichat.tenant.repository;

import com.omnichat.tenant.domain.entity.TenantMember;
import com.omnichat.tenant.domain.entity.TenantMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantMemberRepository extends JpaRepository<TenantMember, String> {

    long countByTenantIdAndStatusIn(String tenantId, java.util.List<TenantMemberStatus> statuses);

    boolean existsByTenantIdAndEmail(String tenantId, String email);

    Optional<TenantMember> findByTenantIdAndEmail(String tenantId, String email);
}
