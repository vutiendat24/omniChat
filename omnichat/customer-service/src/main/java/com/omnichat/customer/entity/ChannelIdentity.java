package com.omnichat.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Value Object (promoted to Entity for persistence): ChannelIdentity
 * Per DDD_Omnichannel_Chat_Management_v1.md §3.3
 *
 * Represents a customer's identity on a specific external platform.
 * The UNIQUE constraint (platform, external_user_id) ensures one identity per platform user.
 */
@Entity
@Table(name = "channel_identities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelIdentity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "external_user_id", nullable = false)
    private String externalUserId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Platform {
        FACEBOOK, ZALO, SHOPEE, TIKTOK
    }
}
