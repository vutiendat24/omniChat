package com.omnichat.customer.repository;

import com.omnichat.customer.entity.ChannelIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelIdentityRepository extends JpaRepository<ChannelIdentity, String> {

    /**
     * Find identity by platform and external user ID.
     * Used for lookup when a webhook arrives with a platform-specific sender ID.
     */
    Optional<ChannelIdentity> findByPlatformAndExternalUserId(
            ChannelIdentity.Platform platform, String externalUserId);

    /**
     * Find all identities belonging to a customer.
     */
    List<ChannelIdentity> findByCustomerId(String customerId);

    /**
     * Check if an identity exists for a given platform and external user ID.
     */
    boolean existsByPlatformAndExternalUserId(
            ChannelIdentity.Platform platform, String externalUserId);
}
