package com.omnichat.conversation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @Column(length = 255)
    private String id;

    @Column(name = "conversation_id", nullable = false, length = 36)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType;

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "content_attachments", columnDefinition = "JSON")
    private String contentAttachments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "sent_at")
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    public enum SenderType {
        CUSTOMER, AGENT, SYSTEM
    }

    public enum MessageStatus {
        SENT, DELIVERED, READ, FAILED
    }
}
