package com.omnichat.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String name; // e.g. ROLE_ADMIN, ROLE_AGENT, ROLE_SUPERVISOR

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;
}
