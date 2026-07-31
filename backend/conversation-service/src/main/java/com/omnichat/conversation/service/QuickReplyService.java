package com.omnichat.conversation.service;

import com.omnichat.conversation.dto.QuickReplyDto;
import com.omnichat.conversation.dto.QuickReplyRequest;
import com.omnichat.conversation.entity.QuickReply;
import com.omnichat.conversation.repository.QuickReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuickReplyService {

    private final QuickReplyRepository quickReplyRepository;

    public List<QuickReplyDto> getAvailableQuickReplies(Long tenantId, Long agentId) {
        return quickReplyRepository.findAvailableQuickReplies(tenantId, agentId)
                .stream()
                .map(QuickReplyDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public QuickReplyDto createQuickReply(Long tenantId, Long agentId, String userRole, QuickReplyRequest request) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole) || "SUPERVISOR".equalsIgnoreCase(userRole);
        boolean isGlobal = Boolean.TRUE.equals(request.getIsGlobal());

        if (isGlobal && !isAdmin) {
            throw new IllegalArgumentException("Only admins can create global quick replies");
        }

        if (isGlobal) {
            if (quickReplyRepository.existsByTenantIdAndShortcutAndIsGlobalTrue(tenantId, request.getShortcut())) {
                throw new IllegalArgumentException("Phím tắt này đã tồn tại trong toàn hệ thống");
            }
        } else {
            if (quickReplyRepository.existsByAgentIdAndShortcut(agentId, request.getShortcut())) {
                throw new IllegalArgumentException("Phím tắt này đã tồn tại");
            }
        }

        QuickReply quickReply = QuickReply.builder()
                .tenantId(tenantId)
                .agentId(isGlobal ? null : agentId)
                .shortcut(request.getShortcut())
                .content(request.getContent())
                .isGlobal(isGlobal)
                .build();

        return QuickReplyDto.fromEntity(quickReplyRepository.save(quickReply));
    }

    @Transactional
    public QuickReplyDto updateQuickReply(Long tenantId, Long agentId, String userRole, Long id, QuickReplyRequest request) {
        QuickReply quickReply = quickReplyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quick reply not found"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole) || "SUPERVISOR".equalsIgnoreCase(userRole);
        
        if (Boolean.TRUE.equals(quickReply.getIsGlobal()) && !isAdmin) {
            throw new IllegalArgumentException("Only admins can update global quick replies");
        }
        
        if (Boolean.FALSE.equals(quickReply.getIsGlobal()) && !quickReply.getAgentId().equals(agentId)) {
            throw new IllegalArgumentException("Cannot update another agent's quick reply");
        }

        // Check shortcut uniqueness if changed
        if (!quickReply.getShortcut().equals(request.getShortcut())) {
            if (quickReply.getIsGlobal()) {
                if (quickReplyRepository.existsByTenantIdAndShortcutAndIsGlobalTrue(tenantId, request.getShortcut())) {
                    throw new IllegalArgumentException("Phím tắt này đã tồn tại trong toàn hệ thống");
                }
            } else {
                if (quickReplyRepository.existsByAgentIdAndShortcut(agentId, request.getShortcut())) {
                    throw new IllegalArgumentException("Phím tắt này đã tồn tại");
                }
            }
        }

        quickReply.setShortcut(request.getShortcut());
        quickReply.setContent(request.getContent());

        return QuickReplyDto.fromEntity(quickReplyRepository.save(quickReply));
    }

    @Transactional
    public void deleteQuickReply(Long tenantId, Long agentId, String userRole, Long id) {
        QuickReply quickReply = quickReplyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quick reply not found"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole) || "SUPERVISOR".equalsIgnoreCase(userRole);
        
        if (Boolean.TRUE.equals(quickReply.getIsGlobal()) && !isAdmin) {
            throw new IllegalArgumentException("Only admins can delete global quick replies");
        }
        
        if (Boolean.FALSE.equals(quickReply.getIsGlobal()) && !quickReply.getAgentId().equals(agentId)) {
            throw new IllegalArgumentException("Cannot delete another agent's quick reply");
        }

        quickReplyRepository.delete(quickReply);
    }
}
