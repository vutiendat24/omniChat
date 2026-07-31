package com.omnichat.conversation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "private_reply_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivateReplyRecord {

    @Id
    @Column(name = "comment_id", length = 255)
    private String commentId;

    @Column(name = "page_id", nullable = false, length = 255)
    private String pageId;

    @Column(name = "conversation_id", nullable = false, length = 36)
    private String conversationId;

    @Column(name = "message_id", nullable = false, length = 36)
    private String messageId;

    @Column(name = "agent_id", nullable = false, length = 255)
    private String agentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReplyStatus status = ReplyStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ReplyStatus {
        PENDING, SUCCESS, FAILED
    }
}
