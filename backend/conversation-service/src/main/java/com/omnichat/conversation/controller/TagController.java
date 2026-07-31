package com.omnichat.conversation.controller;

import com.omnichat.conversation.dto.TagCreateRequest;
import com.omnichat.conversation.dto.TagDto;
import com.omnichat.conversation.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<List<TagDto>> getTags(
            @RequestHeader(value = "X-Tenant-Id", required = false, defaultValue = "1") Long tenantId) {
        return ResponseEntity.ok(tagService.getTagsByTenant(tenantId));
    }

    @PostMapping
    public ResponseEntity<TagDto> createTag(
            @RequestHeader(value = "X-Tenant-Id", required = false, defaultValue = "1") Long tenantId,
            @Valid @RequestBody TagCreateRequest request) {
        return ResponseEntity.ok(tagService.createTag(tenantId, request));
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(
            @RequestHeader(value = "X-Tenant-Id", required = false, defaultValue = "1") Long tenantId,
            @PathVariable Long tagId) {
        tagService.deleteTag(tenantId, tagId);
        return ResponseEntity.noContent().build();
    }
}
