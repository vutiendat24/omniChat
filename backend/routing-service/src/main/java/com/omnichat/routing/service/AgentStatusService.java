package com.omnichat.routing.service;

import com.omnichat.routing.dto.AgentStatusRequest;
import com.omnichat.routing.dto.AgentStatusResponse;
import com.omnichat.routing.entity.Agent;
import com.omnichat.routing.entity.AgentRoutingProfile;
import com.omnichat.routing.repository.AgentRepository;
import com.omnichat.routing.repository.AgentRoutingProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Task 4.2.1.1 - Agent Status Service
 * Handles agent status updates with dual-write to MySQL (source of truth) and Redis (cache for routing).
 *
 * Redis key design:
 * - "routing:agent:{agentId}:profile" -> Hash { status, currentWorkload, maxCapacity }
 * - "routing:agents:online"           -> Set of agentIds currently ONLINE
 *
 * This ensures the routing algorithm can quickly look up available agents from Redis
 * without querying MySQL on every incoming conversation event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentStatusService {

    private final AgentRepository agentRepository;
    private final AgentRoutingProfileRepository routingProfileRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis key patterns
    private static final String AGENT_PROFILE_KEY = "routing:agent:%d:profile";
    private static final String AGENTS_ONLINE_KEY = "routing:agents:online";
    private static final Duration PROFILE_TTL = Duration.ofHours(24);

    /**
     * Update agent status in both MySQL and Redis.
     *
     * Flow:
     * 1. Validate agent exists
     * 2. Validate routing profile exists
     * 3. Update status in MySQL (source of truth)
     * 4. Sync status to Redis (cache for fast routing lookups)
     *    - Update agent profile hash
     *    - Add/remove from online agents set
     *    - If going OFFLINE, reset workload to 0
     *
     * @param agentId the agent ID from path parameter
     * @param request the request body containing the new status
     * @return AgentStatusResponse with the updated profile
     * @throws jakarta.persistence.EntityNotFoundException if agent or profile not found
     */
    @Transactional
    public AgentStatusResponse updateAgentStatus(Long agentId, AgentStatusRequest request) {
        // 1. Parse and validate the status enum
        AgentRoutingProfile.AgentStatus newStatus = request.toAgentStatus();

        // 2. Find agent (404 if not found)
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Agent not found with id: " + agentId));

        // 3. Find or create routing profile
        AgentRoutingProfile profile = routingProfileRepository.findById(agentId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Routing profile not found for agent: " + agentId));

        AgentRoutingProfile.AgentStatus oldStatus = profile.getStatus();

        // 4. Update status in MySQL
        profile.setStatus(newStatus);

        // If agent goes OFFLINE, reset workload to 0 (conversations will be redistributed)
        if (newStatus == AgentRoutingProfile.AgentStatus.OFFLINE) {
            profile.setCurrentWorkload(0);
        }

        profile = routingProfileRepository.save(profile);
        log.info("Agent {} status updated: {} -> {} (MySQL)", agentId, oldStatus, newStatus);

        // 5. Sync to Redis
        syncAgentStatusToRedis(agentId, profile);

        return AgentStatusResponse.fromEntities(agent, profile);
    }

    /**
     * Sync agent status to Redis.
     * Updates the agent's profile hash and manages the online agents set.
     */
    private void syncAgentStatusToRedis(Long agentId, AgentRoutingProfile profile) {
        try {
            String profileKey = String.format(AGENT_PROFILE_KEY, agentId);

            // Update agent profile hash in Redis
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("status", profile.getStatus().name());
            profileData.put("currentWorkload", profile.getCurrentWorkload());
            profileData.put("maxCapacity", profile.getMaxCapacity());
            profileData.put("agentId", agentId);

            redisTemplate.opsForHash().putAll(profileKey, profileData);
            redisTemplate.expire(profileKey, PROFILE_TTL);

            // Manage online agents set
            if (profile.getStatus() == AgentRoutingProfile.AgentStatus.ONLINE) {
                redisTemplate.opsForSet().add(AGENTS_ONLINE_KEY, agentId.toString());
                log.info("Agent {} added to online set (Redis)", agentId);
            } else {
                redisTemplate.opsForSet().remove(AGENTS_ONLINE_KEY, agentId.toString());
                log.info("Agent {} removed from online set (Redis)", agentId);
            }

            log.info("Agent {} profile synced to Redis: status={}, workload={}/{}",
                    agentId, profile.getStatus(), profile.getCurrentWorkload(), profile.getMaxCapacity());

        } catch (Exception e) {
            // Redis sync failure should NOT roll back the DB transaction.
            // Log error and allow the DB update to succeed (eventual consistency).
            log.error("Failed to sync agent {} status to Redis. DB is source of truth.", agentId, e);
        }
    }

    /**
     * Get agent status. Tries Redis first, falls back to MySQL.
     * Used by the routing algorithm for fast lookups.
     */
    public AgentStatusResponse getAgentStatus(Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Agent not found with id: " + agentId));

        AgentRoutingProfile profile = routingProfileRepository.findById(agentId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Routing profile not found for agent: " + agentId));

        return AgentStatusResponse.fromEntities(agent, profile);
    }
}
