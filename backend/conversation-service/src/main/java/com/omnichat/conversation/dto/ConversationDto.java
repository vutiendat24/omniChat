package com.omnichat.conversation.dto;

import com.omnichat.conversation.entity.Conversation;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDto {

    private String id;
    private String channelIdentityId;
    private Long channelConnectionId;
    private Long assignedAgentId;
    private Conversation.ConversationStatus status;
    private Boolean isSlABreached;
    private LocalDateTime lastActivityAt;
    private LocalDateTime createdAt;

    public static ConversationDto fromEntity(Conversation entity) {
        return ConversationDto.builder()
                .id(entity.getId())
                .channelIdentityId(entity.getChannelIdentityId())
                .channelConnectionId(entity.getChannelConnectionId())
                .assignedAgentId(entity.getAssignedAgentId())
                .status(entity.getStatus())
                .isSlABreached(entity.getIsSlABreached())
                .lastActivityAt(entity.getLastActivityAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
