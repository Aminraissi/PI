package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MedicalAssistantAnswerResponse {
    private String answer;
    private String aiProvider;
    private String aiModel;
    private String medicalSummary;
    private String lastDisease;
    private Integer recordCount;
    private List<String> usedContext;
}