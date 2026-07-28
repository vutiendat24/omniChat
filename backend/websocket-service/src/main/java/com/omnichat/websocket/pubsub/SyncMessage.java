package com.omnichat.websocket.pubsub;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncMessage implements Serializable {
    private String targetAgentId;
    private String destination; // e.g., "/queue/conversations"
    private Map<String, Object> payload;
    private String sourceNodeId; // To prevent processing own messages if needed
}
