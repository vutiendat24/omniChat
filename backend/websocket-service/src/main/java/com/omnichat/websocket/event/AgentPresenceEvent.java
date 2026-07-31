package com.omnichat.websocket.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPresenceEvent {
    private String agentId;
    private PresenceStatus status;
    private long timestamp;

    public enum PresenceStatus {
        ONLINE,
        OFFLINE
    }
}
