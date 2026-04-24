package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {
    @NotNull
    private Long veterinarianId;
}
