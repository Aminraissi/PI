package org.example.gestionreclamation.dto;

import org.example.gestionreclamation.enums.ReclamationCategory;
import org.example.gestionreclamation.enums.ReclamationPriority;
import org.example.gestionreclamation.enums.ReclamationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ReclamationResponse(
        Long id,
        Long userId,
        String userFullName,
        String userEmail,
        String userRole,
        String subject,
        ReclamationCategory category,
        String description,
        String attachmentUrl,
        String attachmentFileName,
        ReclamationStatus status,
        ReclamationPriority priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt,
        List<ReclamationMessageResponse> messages
) {}
