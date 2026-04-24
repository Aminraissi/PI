package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatConversationResponse {
    private Long id;
    private UserSummaryResponse farmer;
    private UserSummaryResponse veterinarian;
    private UserSummaryResponse otherParticipant;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}
