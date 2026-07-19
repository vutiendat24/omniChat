package com.omnichat.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root: CustomerProfile (per DDD_Omnichannel_Chat_Management_v1.md §3.3)
 *
 * A customer can have multiple ChannelIdentities (e.g., chat via Facebook AND Zalo).
 * The merge logic uses phone_number matching to unify duplicate profiles.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChannelIdentity> channelIdentities = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * DDD Behavior: linkChannelIdentity
     * Adds a new channel identity to this customer profile.
     */
    public void linkChannelIdentity(ChannelIdentity identity) {
        identity.setCustomer(this);
        identity.setCustomerId(this.id);
        this.channelIdentities.add(identity);
    }

    /**
     * DDD Behavior: updateContactInfo
     * Updates CRM contact information.
     */
    public void updateContactInfo(String phoneNumber, String address) {
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            this.phoneNumber = phoneNumber;
        }
        if (address != null && !address.isBlank()) {
            this.address = address;
        }
    }

    /**
     * DDD Behavior: mergeWith
     * Absorb another customer's data into this profile.
     * - Transfers all channel identities from the source to this customer.
     * - Fills in missing contact info from the source (non-destructive merge).
     *
     * @param source the customer profile to merge INTO this one (source will be deleted after)
     */
    public void mergeWith(Customer source) {
        // 1. Fill in missing contact info from source (don't overwrite existing data)
        if ((this.fullName == null || this.fullName.isBlank()) && source.getFullName() != null) {
            this.fullName = source.getFullName();
        }
        if ((this.phoneNumber == null || this.phoneNumber.isBlank()) && source.getPhoneNumber() != null) {
            this.phoneNumber = source.getPhoneNumber();
        }
        if ((this.address == null || this.address.isBlank()) && source.getAddress() != null) {
            this.address = source.getAddress();
        }

        // 2. Transfer all channel identities from source to this customer
        List<ChannelIdentity> identitiesToTransfer = new ArrayList<>(source.getChannelIdentities());
        for (ChannelIdentity identity : identitiesToTransfer) {
            identity.setCustomer(this);
            identity.setCustomerId(this.id);
        }
        this.channelIdentities.addAll(identitiesToTransfer);
        source.getChannelIdentities().clear();
    }
}
