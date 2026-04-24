package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DiagnosticResponse {

    private String animalReference;
    private String animalEspece;

    // Top-3 maladies prédites par le modèle ML
    private List<DiseasePrediction> predictions;

    // Analyse enrichie par Gemini
    private String geminiAnalysis;

    private String disclaimer;

    @Data
    @Builder
    public static class DiseasePrediction {
        private int    rank;
        private String disease;
        private double probability;
    }
}
