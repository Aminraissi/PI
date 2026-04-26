package org.example.servicepret.DTO;

import lombok.Data;
import java.util.List;

@Data
public class FraudAnalysisResult {
    private String globalRisk;
    private int globalScore;
    private boolean fraudConfirmed;
    private String recommendation;
    private String recommendationJustification;
    private String dossierNarrative;
    private List<SuspiciousField> allSuspiciousFields;
    private List<String> crossDocumentInconsistencies;
    private List<String> criticalDocuments;
    private List<DocumentAnalysis> documents;

    // Stats
    private int totalDocuments;
    private int highRiskDocuments;
    private int mediumRiskDocuments;
    private int lowRiskDocuments;

    @Data
    public static class SuspiciousField {
        private String document;
        private String fieldName;
        private String suspiciousValue;
        private String reason;
        private String severity;
    }

    @Data
    public static class DocumentAnalysis {
        private String filename;
        private String docType;
        private String riskLevel;
        private int riskScore;
        private int aiGeneratedProbability;
        private String documentNarrative;
        private Metadata metadata;
        private List<String> fraudSignals;
    }

    @Data
    public static class Metadata {
        private String creator;
        private String producer;
        private String metadataRisk;
    }
}