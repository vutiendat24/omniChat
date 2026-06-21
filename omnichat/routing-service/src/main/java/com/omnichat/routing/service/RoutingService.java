package com.omnichat.routing.service;

import com.omnichat.routing.entity.AgentRoutingProfile;
import com.omnichat.routing.producer.RoutingEventProducer;
import com.omnichat.routing.repository.AgentRoutingProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Task 4.3.2.1 - Routing Service: Round-Robin Assignment Algorithm
 *
 * Core routing logic that assigns UNASSIGNED conversations to available agents.
 *
 * Algorithm (per Architecture_Design_OCM.md §5):
 * 1. Load online agent list from Redis Set "routing:agents:online"
 * 2. Run atomic Round-Robin selection using Redis INCR on "routing:round-robin:index"
 * 3. For the selected agent, verify capacity: currentWorkload < maxCapacity
 *    - If at capacity, try the next agent (up to full cycle)
 * 4. Atomically increment workload via Redis HINCRBY (prevents race conditions
 *    when multiple routing events arrive simultaneously)
 * 5. Sync workload back to MySQL (source of truth)
 * 6. Publish RouteAssigned event to Kafka
 *
 * Redis key design (shared with AgentStatusService):
 * - "routing:agent:{agentId}:profile" → Hash { status, currentWorkload, maxCapacity, agentId }
 * - "routing:agents:online"           → Set of agentId strings
 * - "routing:round-robin:index"       → Integer counter for round-robin position
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AgentRoutingProfileRepository routingProfileRepository;
    private final RoutingEventProducer routingEventProducer;

    // Redis keys (matching AgentStatusService constants)
    private static final String AGENT_PROFILE_KEY = "routing:agent:%d:profile";
    private static final String AGENTS_ONLINE_KEY = "routing:agents:online";
    private static final String ROUND_ROBIN_INDEX_KEY = "routing:round-robin:index";

    /**
     * Main entry point: route an UNASSIGNED conversation to an available agent.
     *
     * @param conversationId the conversation to assign
     */
    public void routeConversation(String conversationId) {
        log.info("Starting routing for conversationId={}", conversationId);

        Long selectedAgentId = findAvailableAgent();

        if (selectedAgentId == null) {
            log.warn("No available agent found for conversationId={}. " +
                    "Conversation remains UNASSIGNED and will be retried on next event.", conversationId);
            return;
        }

        // Atomically increment workload in Redis (+1)
        boolean locked = atomicIncrementWorkload(selectedAgentId);
        if (!locked) {
            log.error("Failed to atomically lock workload for agent {} on conversation {}",
                    selectedAgentId, conversationId);
            return;
        }

        // Sync workload increment to MySQL (source of truth)
        syncWorkloadToMySQL(selectedAgentId);

        // Task 4.3.3.1 - Publish RouteAssigned event
        routingEventProducer.publishRouteAssigned(conversationId, selectedAgentId);

        log.info("Successfully routed conversationId={} to agentId={}", conversationId, selectedAgentId);
    }

    /**
     * Round-Robin algorithm to find an available agent.
     *
     * Steps:
     * 1. Get all online agents from Redis Set
     * 2. Atomically increment the round-robin index (Redis INCR)
     * 3. Use modulo to select an agent from the online list
     * 4. Check if selected agent has available capacity
     * 5. If not, try next agent (cycle through all online agents max once)
     *
     * @return the agentId of the selected agent, or null if none available
     */
    private Long findAvailableAgent() {
        // 1. Get online agents from Redis
        List<Long> onlineAgentIds = getOnlineAgentIds();

        if (onlineAgentIds.isEmpty()) {
            // Fallback: try MySQL if Redis has no data (cold start scenario)
            log.warn("No online agents in Redis cache, falling back to MySQL");
            onlineAgentIds = getOnlineAgentIdsFromMySQL();
        }

        if (onlineAgentIds.isEmpty()) {
            log.warn("No online agents found in either Redis or MySQL");
            return null;
        }

        int agentCount = onlineAgentIds.size();
        log.info("Found {} online agents for routing: {}", agentCount, onlineAgentIds);

        // 2. Atomic round-robin index increment
        Long roundRobinIndex = redisTemplate.opsForValue().increment(ROUND_ROBIN_INDEX_KEY);
        if (roundRobinIndex == null) {
            roundRobinIndex = 0L;
        }

        // 3. Try each agent starting from the round-robin position (full cycle)
        for (int attempt = 0; attempt < agentCount; attempt++) {
            int index = (int) ((roundRobinIndex + attempt) % agentCount);
            Long candidateAgentId = onlineAgentIds.get(index);

            if (hasAvailableCapacity(candidateAgentId)) {
                log.info("Selected agent {} at round-robin index {} (attempt {})",
                        candidateAgentId, index, attempt + 1);
                return candidateAgentId;
            } else {
                log.debug("Agent {} at capacity, trying next", candidateAgentId);
            }
        }

        log.warn("All {} online agents are at full capacity", agentCount);
        return null;
    }

    /**
     * Get list of online agent IDs from Redis Set "routing:agents:online".
     * Returns a stable sorted list for consistent round-robin ordering.
     */
    private List<Long> getOnlineAgentIds() {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(AGENTS_ONLINE_KEY);
            if (members == null || members.isEmpty()) {
                return List.of();
            }

            // Parse and sort for deterministic round-robin order
            List<Long> agentIds = new ArrayList<>();
            for (Object member : members) {
                try {
                    agentIds.add(Long.parseLong(member.toString()));
                } catch (NumberFormatException e) {
                    log.warn("Invalid agent ID in online set: {}", member);
                }
            }
            agentIds.sort(Long::compareTo);
            return agentIds;

        } catch (Exception e) {
            log.error("Failed to get online agents from Redis", e);
            return List.of();
        }
    }

    /**
     * Fallback: get online agents from MySQL when Redis cache is empty (cold start).
     */
    private List<Long> getOnlineAgentIdsFromMySQL() {
        try {
            return routingProfileRepository.findByStatus(AgentRoutingProfile.AgentStatus.ONLINE)
                    .stream()
                    .map(AgentRoutingProfile::getAgentId)
                    .sorted()
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get online agents from MySQL", e);
            return List.of();
        }
    }

    /**
     * Check if an agent has available capacity using Redis Hash.
     * Compares currentWorkload < maxCapacity from "routing:agent:{id}:profile".
     *
     * @param agentId the agent to check
     * @return true if agent can accept more conversations
     */
    private boolean hasAvailableCapacity(Long agentId) {
        try {
            String profileKey = String.format(AGENT_PROFILE_KEY, agentId);

            Object workloadObj = redisTemplate.opsForHash().get(profileKey, "currentWorkload");
            Object capacityObj = redisTemplate.opsForHash().get(profileKey, "maxCapacity");

            if (workloadObj == null || capacityObj == null) {
                // Redis miss → fallback to MySQL
                log.debug("Redis cache miss for agent {} profile, checking MySQL", agentId);
                return hasAvailableCapacityFromMySQL(agentId);
            }

            int currentWorkload = Integer.parseInt(workloadObj.toString());
            int maxCapacity = Integer.parseInt(capacityObj.toString());

            boolean available = currentWorkload < maxCapacity;
            log.debug("Agent {} capacity check: {}/{} → {}",
                    agentId, currentWorkload, maxCapacity, available ? "AVAILABLE" : "FULL");

            return available;

        } catch (Exception e) {
            log.error("Failed to check capacity for agent {} from Redis, falling back to MySQL", agentId, e);
            return hasAvailableCapacityFromMySQL(agentId);
        }
    }

    /**
     * Fallback capacity check from MySQL.
     */
    private boolean hasAvailableCapacityFromMySQL(Long agentId) {
        return routingProfileRepository.findById(agentId)
                .map(profile -> profile.getCurrentWorkload() < profile.getMaxCapacity())
                .orElse(false);
    }

    /**
     * Atomically increment the agent's workload in Redis using HINCRBY.
     *
     * HINCRBY is atomic — it prevents race conditions when multiple routing
     * events arrive simultaneously and try to assign to the same agent.
     * If the incremented workload exceeds maxCapacity, we rollback (HINCRBY -1).
     *
     * @param agentId the agent whose workload to increment
     * @return true if workload was successfully incremented within capacity
     */
    private boolean atomicIncrementWorkload(Long agentId) {
        try {
            String profileKey = String.format(AGENT_PROFILE_KEY, agentId);

            // Atomic increment: HINCRBY routing:agent:{id}:profile currentWorkload 1
            Long newWorkload = redisTemplate.opsForHash().increment(profileKey, "currentWorkload", 1);

            // Double-check: if increment pushed us over capacity, rollback
            Object capacityObj = redisTemplate.opsForHash().get(profileKey, "maxCapacity");
            int maxCapacity = capacityObj != null ? Integer.parseInt(capacityObj.toString()) : 10;

            if (newWorkload > maxCapacity) {
                // Rollback: another thread beat us to the last slot
                redisTemplate.opsForHash().increment(profileKey, "currentWorkload", -1);
                log.warn("Agent {} workload exceeded capacity after atomic increment ({}/{}), rolled back",
                        agentId, newWorkload, maxCapacity);
                return false;
            }

            log.info("Atomic workload increment for agent {}: {} → {} (capacity: {})",
                    agentId, newWorkload - 1, newWorkload, maxCapacity);
            return true;

        } catch (Exception e) {
            log.error("Failed to atomically increment workload for agent {}", agentId, e);
            return false;
        }
    }

    /**
     * Sync the Redis workload value back to MySQL (source of truth).
     * This ensures MySQL stays consistent even if Redis operations succeed
     * but the service crashes before DB update.
     */
    @Transactional
    public void syncWorkloadToMySQL(Long agentId) {
        try {
            String profileKey = String.format(AGENT_PROFILE_KEY, agentId);
            Object workloadObj = redisTemplate.opsForHash().get(profileKey, "currentWorkload");

            int redisWorkload = workloadObj != null ? Integer.parseInt(workloadObj.toString()) : 0;

            routingProfileRepository.findById(agentId).ifPresent(profile -> {
                profile.setCurrentWorkload(redisWorkload);
                routingProfileRepository.save(profile);
                log.info("Synced workload to MySQL for agent {}: workload={}", agentId, redisWorkload);
            });

        } catch (Exception e) {
            // MySQL sync failure is non-fatal; Redis is authoritative for workload during routing.
            // A background reconciliation job can fix drift later.
            log.error("Failed to sync workload to MySQL for agent {}. Redis remains authoritative.", agentId, e);
        }
    }
}
