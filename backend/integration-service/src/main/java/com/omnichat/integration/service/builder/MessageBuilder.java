package com.omnichat.integration.service.builder;

import com.omnichat.integration.dto.unified.UnifiedMessage;
import java.util.Map;

public interface MessageBuilder {
    boolean supports(String platform);
    
    /**
     * @return Map containing payload details specific to the platform
     */
    Map<String, Object> buildPayload(UnifiedMessage message);
}
