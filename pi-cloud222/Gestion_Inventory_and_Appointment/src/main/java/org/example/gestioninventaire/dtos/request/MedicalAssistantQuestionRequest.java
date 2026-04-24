package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MedicalAssistantQuestionRequest {

    @NotBlank(message = "La question est obligatoire")
    private String question;
}