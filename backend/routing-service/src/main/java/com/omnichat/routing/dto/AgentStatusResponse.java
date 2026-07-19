package com.omnichat.routing.dto;

import com.omnichat.routing.entity.Agent;
import com.omnichat.routing.entity.AgentRoutingProfile;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentStatusResponse {

    private Long agentId;
    private String fullName;
    private String email;
    private String role;
    private String status;
    private Integer currentWorkload;
    private Integer maxCapacity;
    private LocalDateTime updatedAt;

    public static AgentStatusResponse fromEntities(Agent agent, AgentRoutingProfile profile) {
        return AgentStatusResponse.builder()
                .agentId(agent.getId())
                .fullName(agent.getFullName())
                .email(agent.getEmail())
                .role(agent.getRole().name())
                .status(profile.getStatus().name())
                .currentWorkload(profile.getCurrentWorkload())
                .maxCapacity(profile.getMaxCapacity())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
