package com.omnichat.integration.service.parser;

import com.omnichat.integration.dto.unified.UnifiedMessage;
import java.util.List;

public interface WebhookParser {
    boolean supports(String platform);
    List<UnifiedMessage> parse(String rawPayload) throws Exception;
}
