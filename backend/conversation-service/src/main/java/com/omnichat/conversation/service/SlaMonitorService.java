package com.omnichat.conversation.service;

import com.omnichat.conversation.entity.Conversation;
import com.omnichat.conversation.producer.ConversationEventProducer;
import com.omnichat.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaMonitorService {

    private final ConversationRepository conversationRepository;
    private final ConversationEventProducer conversationEventProducer;

    /**
     * Runs every minute to check for SLA breaches.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkSlaBreaches() {
        LocalDateTime now = LocalDateTime.now();

        // Find conversations that are OPEN, not yet responded, not yet breached, and passed SLA
        List<Conversation> breachedConversations = conversationRepository.findBreachedConversations(now);

        for (Conversation conversation : breachedConversations) {
            log.warn("Conversation {} breached SLA. Due at: {}", conversation.getId(), conversation.getSlaDueAt());
            conversation.setIsSlABreached(true);
            
            // Publish event to Kafka (notification-service / realtime-service)
            // Need a new method in producer or use existing, here we assume publishSlaBreached exists or we can just log
            // For now, we will add a method in producer or just log if we don't want to change producer API
        }

        if (!breachedConversations.isEmpty()) {
            conversationRepository.saveAll(breachedConversations);
        }
    }
    
    /**
     * Runs every minute to check for SLA warnings (e.g. 5 minutes before breach)
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkSlaWarnings() {
        LocalDateTime warningTime = LocalDateTime.now().plusMinutes(5);
        LocalDateTime now = LocalDateTime.now();
        
        // Find conversations due between now and 5 minutes from now, not breached, not responded
        List<Conversation> warningConversations = conversationRepository.findSlaWarningConversations(now, warningTime);
        
        for (Conversation conversation : warningConversations) {
            log.info("Conversation {} is near SLA breach (Due at {})", conversation.getId(), conversation.getSlaDueAt());
            // Fire event for UI red warning
        }
    }
}
