package com.omnichat.conversation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "channel_identity_id", nullable = false, length = 36)
    private String channelIdentityId;

    @Column(name = "channel_connection_id", nullable = false)
    private Long channelConnectionId;

    @Column(name = "assigned_agent_id")
    private Long assignedAgentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.OPEN;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_preview")
    private String lastMessagePreview;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_avatar")
    private String customerAvatar;

    @Column(name = "is_sla_breached")
    @Builder.Default
    private Boolean isSlABreached = false;

    @Version
    private Long version;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ConversationStatus {
        OPEN, PENDING, RESOLVED, SPAM
    }
}
