package org.example.gestioninventaire.dtos.request;

import lombok.Data;

@Data
public class DiagnosticAssistantChatRequest {
    private String animalType;
    private String symptom1;
    private String symptom2;
    private String symptom3;
    private String duration;
    private String bodyTemperature;
    private String question;
}