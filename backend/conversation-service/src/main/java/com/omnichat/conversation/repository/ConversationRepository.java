package com.omnichat.conversation.repository;

import com.omnichat.conversation.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String>, JpaSpecificationExecutor<Conversation> {

    Optional<Conversation> findByChannelIdentityIdAndStatus(
            String channelIdentityId, Conversation.ConversationStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Conversation c WHERE c.status = 'OPEN' AND c.firstRespondedAt IS NULL AND c.isSlABreached = false AND c.slaDueAt < :now")
    List<Conversation> findBreachedConversations(java.time.LocalDateTime now);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Conversation c WHERE c.status = 'OPEN' AND c.firstRespondedAt IS NULL AND c.isSlABreached = false AND c.slaDueAt >= :now AND c.slaDueAt <= :warningTime")
    List<Conversation> findSlaWarningConversations(java.time.LocalDateTime now, java.time.LocalDateTime warningTime);

    Page<Conversation> findByStatus(Conversation.ConversationStatus status, Pageable pageable);

    Page<Conversation> findByAssignedAgentId(Long agentId, Pageable pageable);

    Page<Conversation> findByStatusAndAssignedAgentId(
            Conversation.ConversationStatus status, Long agentId, Pageable pageable);
}
