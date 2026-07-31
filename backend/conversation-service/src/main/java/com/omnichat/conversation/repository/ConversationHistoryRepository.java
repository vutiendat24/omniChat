package com.omnichat.conversation.repository;

import com.omnichat.conversation.entity.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, Long> {
}
