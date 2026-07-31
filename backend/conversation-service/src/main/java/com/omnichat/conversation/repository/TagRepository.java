package com.omnichat.conversation.repository;

import com.omnichat.conversation.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByTenantId(Long tenantId);
    Optional<Tag> findByTenantIdAndName(Long tenantId, String name);
    void deleteByTenantIdAndId(Long tenantId, Long id);
}
