package com.omnichat.conversation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "quick_replies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_shortcut_global", columnNames = {"tenant_id", "shortcut", "is_global"}),
        @UniqueConstraint(name = "uk_agent_shortcut", columnNames = {"agent_id", "shortcut"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(length = 20, nullable = false)
    private String shortcut;

    @Column(length = 2000, nullable = false)
    private String content;

    @Column(name = "is_global")
    @Builder.Default
    private Boolean isGlobal = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
