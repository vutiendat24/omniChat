package com.omnichat.conversation.service;

import com.omnichat.conversation.entity.Conversation;
import com.omnichat.conversation.producer.ConversationEventProducer;
import com.omnichat.conversation.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaMonitorServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationEventProducer conversationEventProducer;

    @InjectMocks
    private SlaMonitorService slaMonitorService;

    @Test
    void checkSlaBreaches_ShouldMarkBreachedConversations() {
        Conversation conv = new Conversation();
        conv.setId("conv-1");
        conv.setSlaDueAt(LocalDateTime.now().minusMinutes(5));
        conv.setIsSlABreached(false);

        when(conversationRepository.findBreachedConversations(any())).thenReturn(List.of(conv));

        slaMonitorService.checkSlaBreaches();

        assertTrue(conv.getIsSlABreached());
        verify(conversationRepository).saveAll(anyList());
    }

    @Test
    void checkSlaWarnings_ShouldFindWarningConversations() {
        Conversation conv = new Conversation();
        conv.setId("conv-2");
        conv.setSlaDueAt(LocalDateTime.now().plusMinutes(2));

        when(conversationRepository.findSlaWarningConversations(any(), any())).thenReturn(List.of(conv));

        slaMonitorService.checkSlaWarnings();

        verify(conversationRepository).findSlaWarningConversations(any(), any());
    }
}
