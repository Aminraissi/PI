package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderBySentAtAsc(Long conversationId);
    ChatMessage findTopByConversationIdOrderBySentAtDesc(Long conversationId);
    long countByConversationIdAndReceiverId(Long conversationId, Long receiverId);
}
