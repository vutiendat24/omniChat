package com.omnichat.conversation.service;

import com.omnichat.conversation.dto.TagCreateRequest;
import com.omnichat.conversation.dto.TagDto;
import com.omnichat.conversation.entity.Tag;
import com.omnichat.conversation.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public List<TagDto> getTagsByTenant(Long tenantId) {
        return tagRepository.findByTenantId(tenantId).stream()
                .map(TagDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public TagDto createTag(Long tenantId, TagCreateRequest request) {
        // Idempotent creation or return existing
        return tagRepository.findByTenantIdAndName(tenantId, request.getName())
                .map(TagDto::fromEntity)
                .orElseGet(() -> {
                    Tag newTag = Tag.builder()
                            .tenantId(tenantId)
                            .name(request.getName())
                            .color(request.getColor() != null ? request.getColor() : "#000000")
                            .build();
                    return TagDto.fromEntity(tagRepository.save(newTag));
                });
    }

    @Transactional
    public void deleteTag(Long tenantId, Long tagId) {
        tagRepository.deleteByTenantIdAndId(tenantId, tagId);
    }
}
