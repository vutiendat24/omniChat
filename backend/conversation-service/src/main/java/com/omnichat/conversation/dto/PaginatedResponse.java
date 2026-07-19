package com.omnichat.conversation.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginatedResponse<T> {

    private List<T> data;
    private Meta meta;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Meta {
        private int currentPage;
        private int totalPages;
        private long totalItems;
    }
}
