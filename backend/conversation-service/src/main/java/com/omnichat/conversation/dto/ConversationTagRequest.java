package com.omnichat.conversation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationTagRequest {
    private Long tagId;
    private String tagName; // Used if tagId is null to create on-the-fly
    private String action; // ADD or REMOVE
}
