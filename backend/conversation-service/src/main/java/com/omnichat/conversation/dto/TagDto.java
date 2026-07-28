package com.omnichat.conversation.dto;

import com.omnichat.conversation.entity.Tag;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagDto {
    private Long id;
    private String name;
    private String color;
    private LocalDateTime createdAt;

    public static TagDto fromEntity(Tag entity) {
        return TagDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .color(entity.getColor())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
