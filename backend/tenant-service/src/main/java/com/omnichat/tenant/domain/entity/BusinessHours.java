package com.omnichat.tenant.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_hours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessHours {

    @Id
    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "schedule_json", columnDefinition = "JSON", nullable = false)
    private String scheduleJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
