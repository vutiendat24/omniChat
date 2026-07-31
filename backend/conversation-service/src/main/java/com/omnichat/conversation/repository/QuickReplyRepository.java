package com.omnichat.conversation.repository;

import com.omnichat.conversation.entity.QuickReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuickReplyRepository extends JpaRepository<QuickReply, Long> {
    List<QuickReply> findByTenantId(Long tenantId);
    @org.springframework.data.jpa.repository.Query("SELECT q FROM QuickReply q WHERE q.tenantId = :tenantId AND (q.isGlobal = true OR q.agentId = :agentId)")
    List<QuickReply> findAvailableQuickReplies(Long tenantId, Long agentId);
    
    boolean existsByTenantIdAndShortcutAndIsGlobalTrue(Long tenantId, String shortcut);
    boolean existsByAgentIdAndShortcut(Long agentId, String shortcut);
}
