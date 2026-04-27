package org.example.gestionreclamation.dto;

import org.example.gestionreclamation.enums.SenderRole;

import java.time.LocalDateTime;

public record ReclamationMessageResponse(
        Long id,
        Long senderId,
        String senderName,
        SenderRole senderRole,
        String message,
        LocalDateTime createdAt
) {}
