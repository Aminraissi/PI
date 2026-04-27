package org.example.gestionreclamation.dto;

import org.example.gestionreclamation.enums.ReclamationCategory;

public record CorrectDescriptionRequest(
        String subject,
        String category,
        String description
) {}
