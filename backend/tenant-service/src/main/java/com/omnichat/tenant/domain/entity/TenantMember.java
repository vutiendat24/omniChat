package com.omnichat.tenant.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantMember {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "role_id", nullable = false, length = 50)
    private String roleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TenantMemberStatus status = TenantMemberStatus.PENDING;

    @Column(name = "invited_at", updatable = false)
    @Builder.Default
    private LocalDateTime invitedAt = LocalDateTime.now();

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
