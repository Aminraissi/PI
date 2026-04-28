package org.example.gestionreclamation.dto;

import org.example.gestionreclamation.enums.SenderRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMessageRequest(
        @NotNull Long senderId,
        @NotNull SenderRole senderRole,
        @NotBlank String message
) {}
