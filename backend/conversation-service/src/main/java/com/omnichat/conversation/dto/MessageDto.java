package com.omnichat.conversation.dto;

import com.omnichat.conversation.entity.Message;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {

    private String id;
    private String conversationId;
    private Message.SenderType senderType;
    private String senderId;
    private String contentText;
    private List<String> contentAttachments;
    private Message.MessageStatus status;
    private LocalDateTime sentAt;

    public static MessageDto fromEntity(Message entity) {
        // Parse JSON array string to List<String> for content_attachments
        List<String> attachments = null;
        if (entity.getContentAttachments() != null && !entity.getContentAttachments().isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                attachments = mapper.readValue(entity.getContentAttachments(),
                        mapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (Exception e) {
                attachments = List.of();
            }
        }

        return MessageDto.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .senderType(entity.getSenderType())
                .senderId(entity.getSenderId())
                .contentText(entity.getContentText())
                .contentAttachments(attachments)
                .status(entity.getStatus())
                .sentAt(entity.getSentAt())
                .build();
    }
}
