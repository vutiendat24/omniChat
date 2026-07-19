package com.omnichat.routing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_routing_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentRoutingProfile {

    @Id
    @Column(name = "agent_id")
    private Long agentId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AgentStatus status = AgentStatus.OFFLINE;

    @Column(name = "current_workload")
    @Builder.Default
    private Integer currentWorkload = 0;

    @Column(name = "max_capacity")
    @Builder.Default
    private Integer maxCapacity = 10;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AgentStatus {
        ONLINE, BUSY, OFFLINE
    }
}
