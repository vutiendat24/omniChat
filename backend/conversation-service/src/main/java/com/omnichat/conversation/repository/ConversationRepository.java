package com.omnichat.conversation.repository;

import com.omnichat.conversation.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    Optional<Conversation> findByChannelIdentityIdAndStatus(
            String channelIdentityId, Conversation.ConversationStatus status);

    Page<Conversation> findByStatus(Conversation.ConversationStatus status, Pageable pageable);

    Page<Conversation> findByAssignedAgentId(Long agentId, Pageable pageable);

    Page<Conversation> findByStatusAndAssignedAgentId(
            Conversation.ConversationStatus status, Long agentId, Pageable pageable);
}
