package org.example.gestioninventaire.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageChatbotResponse {
    private String predictedLabel;
    private Double confidence;
    private List<ImagePrediction> predictions;
    private String analysis;
    private String disclaimer;
    private Map<String, Object> trainingSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImagePrediction {
        private Integer rank;
        private String disease;
        private Double probability;
    }
}
