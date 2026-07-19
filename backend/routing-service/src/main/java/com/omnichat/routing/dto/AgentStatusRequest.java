package com.omnichat.routing.dto;

import com.omnichat.routing.entity.AgentRoutingProfile;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentStatusRequest {

    private String status;

    /**
     * Validate and parse the status string to enum.
     * @return the parsed AgentStatus enum value
     * @throws IllegalArgumentException if the status is null or invalid
     */
    public AgentRoutingProfile.AgentStatus toAgentStatus() {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        try {
            return AgentRoutingProfile.AgentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status: '" + status + "'. Valid values are: ONLINE, BUSY, OFFLINE");
        }
    }
}
