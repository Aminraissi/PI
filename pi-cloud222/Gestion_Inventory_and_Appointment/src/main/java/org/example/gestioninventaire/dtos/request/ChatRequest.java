package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequest {

    @NotNull
    private Long animalId;

    @NotBlank
    private String question;
}
