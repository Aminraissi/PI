package org.example.gestionreclamation.dto;

import org.example.gestionreclamation.enums.ReclamationCategory;
import org.example.gestionreclamation.enums.ReclamationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReclamationRequest(
        @NotNull Long userId,
        @NotBlank String subject,
        @NotNull ReclamationCategory category,
        @NotBlank String description,
        ReclamationPriority priority
) {}
