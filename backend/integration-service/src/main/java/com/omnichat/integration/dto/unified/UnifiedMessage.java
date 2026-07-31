package com.omnichat.integration.dto.unified;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedMessage {
    private String platform;
    private String channelId;
    private Sender sender;
    private MessageType messageType;
    private Content content;
    private Long timestamp;
    private String rawPayloadRef;
    
    public enum MessageType {
        TEXT, IMAGE, VIDEO, FILE, STICKER, EVENT, READ_RECEIPT, UNSUPPORTED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sender {
        private String platformUserId;
        private String name;
        private String avatar;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String text;
        private List<Attachment> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String type; // e.g. "image", "video", "file"
        private String url;
    }
}
