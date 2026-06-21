package com.omnichat.routing.repository;

import com.omnichat.routing.entity.AgentRoutingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentRoutingProfileRepository extends JpaRepository<AgentRoutingProfile, Long> {

    /**
     * Find all agents currently online (used by routing algorithm).
     */
    List<AgentRoutingProfile> findByStatus(AgentRoutingProfile.AgentStatus status);

    /**
     * Find all online agents with available capacity (workload < maxCapacity).
     */
    List<AgentRoutingProfile> findByStatusAndCurrentWorkloadLessThan(
            AgentRoutingProfile.AgentStatus status, Integer maxWorkload);
}
