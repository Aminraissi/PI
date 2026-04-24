package org.exemple.assistance_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMResponseDTO {
    private String diagnostic;
    private double probabilite;
    private String recommandations;
    private boolean besoinExpert;
}
