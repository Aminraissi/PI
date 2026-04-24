package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DiagnosticRequest {

    @NotNull
    private Long animalId;

    @NotBlank
    private String symptom1;

    private String symptom2;
    private String symptom3;

    @NotBlank
    private String duration;

    private String bodyTemperature = "39.0C";

    private String question;
}
