package com.omnichat.conversation.repository;

import com.omnichat.conversation.entity.PrivateReplyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivateReplyRecordRepository extends JpaRepository<PrivateReplyRecord, String> {
}
