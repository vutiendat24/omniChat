package com.omnichat.conversation.dto;

import com.omnichat.conversation.entity.QuickReply;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickReplyDto {
    private Long id;
    private Long tenantId;
    private Long agentId;
    private String shortcut;
    private String content;
    private Boolean isGlobal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static QuickReplyDto fromEntity(QuickReply entity) {
        return QuickReplyDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .agentId(entity.getAgentId())
                .shortcut(entity.getShortcut())
                .content(entity.getContent())
                .isGlobal(entity.getIsGlobal())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
