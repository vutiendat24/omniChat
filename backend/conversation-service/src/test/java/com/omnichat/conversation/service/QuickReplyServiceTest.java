package com.omnichat.conversation.service;

import com.omnichat.conversation.dto.QuickReplyDto;
import com.omnichat.conversation.dto.QuickReplyRequest;
import com.omnichat.conversation.entity.QuickReply;
import com.omnichat.conversation.repository.QuickReplyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuickReplyServiceTest {

    @Mock
    private QuickReplyRepository quickReplyRepository;

    @InjectMocks
    private QuickReplyService quickReplyService;

    @Test
    void getAvailableQuickReplies_ShouldReturnGlobalAndPersonal() {
        QuickReply qrGlobal = QuickReply.builder().id(1L).tenantId(10L).isGlobal(true).shortcut("/g").content("Global").build();
        QuickReply qrPersonal = QuickReply.builder().id(2L).tenantId(10L).agentId(5L).isGlobal(false).shortcut("/p").content("Personal").build();

        when(quickReplyRepository.findAvailableQuickReplies(10L, 5L)).thenReturn(List.of(qrGlobal, qrPersonal));

        List<QuickReplyDto> result = quickReplyService.getAvailableQuickReplies(10L, 5L);
        assertEquals(2, result.size());
    }

    @Test
    void createQuickReply_WhenGlobalAndAdmin_ShouldCreate() {
        QuickReplyRequest req = new QuickReplyRequest();
        req.setIsGlobal(true);
        req.setShortcut("/hi");
        req.setContent("Hello");

        when(quickReplyRepository.existsByTenantIdAndShortcutAndIsGlobalTrue(10L, "/hi")).thenReturn(false);
        when(quickReplyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuickReplyDto result = quickReplyService.createQuickReply(10L, 5L, "ADMIN", req);
        assertTrue(result.getIsGlobal());
        assertEquals("/hi", result.getShortcut());
    }

    @Test
    void createQuickReply_WhenGlobalAndAgent_ShouldThrowException() {
        QuickReplyRequest req = new QuickReplyRequest();
        req.setIsGlobal(true);
        req.setShortcut("/hi");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                () -> quickReplyService.createQuickReply(10L, 5L, "AGENT", req));
        
        assertEquals("Only admins can create global quick replies", ex.getMessage());
    }

    @Test
    void createQuickReply_WhenPersonalDuplicate_ShouldThrowException() {
        QuickReplyRequest req = new QuickReplyRequest();
        req.setIsGlobal(false);
        req.setShortcut("/hi");

        when(quickReplyRepository.existsByAgentIdAndShortcut(5L, "/hi")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                () -> quickReplyService.createQuickReply(10L, 5L, "AGENT", req));
        
        assertEquals("Phím tắt này đã tồn tại", ex.getMessage());
    }
}
