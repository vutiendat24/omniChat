package com.omnichat.conversation.service;

import com.omnichat.conversation.dto.TagCreateRequest;
import com.omnichat.conversation.dto.TagDto;
import com.omnichat.conversation.entity.Tag;
import com.omnichat.conversation.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    @Test
    void getTagsByTenant_ShouldReturnTags() {
        Tag tag1 = Tag.builder().id(1L).tenantId(10L).name("VIP").color("#FF0000").build();
        when(tagRepository.findByTenantId(10L)).thenReturn(List.of(tag1));

        List<TagDto> result = tagService.getTagsByTenant(10L);

        assertEquals(1, result.size());
        assertEquals("VIP", result.get(0).getName());
    }

    @Test
    void createTag_WhenTagExists_ShouldReturnExistingTag() {
        TagCreateRequest req = new TagCreateRequest();
        req.setName("URGENT");
        Tag existingTag = Tag.builder().id(2L).tenantId(10L).name("URGENT").build();

        when(tagRepository.findByTenantIdAndName(10L, "URGENT")).thenReturn(Optional.of(existingTag));

        TagDto result = tagService.createTag(10L, req);

        assertEquals(2L, result.getId());
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void createTag_WhenTagDoesNotExist_ShouldCreateNewTag() {
        TagCreateRequest req = new TagCreateRequest();
        req.setName("NEW");
        Tag newTag = Tag.builder().id(3L).tenantId(10L).name("NEW").color("#000000").build();

        when(tagRepository.findByTenantIdAndName(10L, "NEW")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(newTag);

        TagDto result = tagService.createTag(10L, req);

        assertEquals(3L, result.getId());
        assertEquals("NEW", result.getName());
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void deleteTag_ShouldDeleteById() {
        tagService.deleteTag(10L, 5L);
        verify(tagRepository).deleteByTenantIdAndId(10L, 5L);
    }
}
