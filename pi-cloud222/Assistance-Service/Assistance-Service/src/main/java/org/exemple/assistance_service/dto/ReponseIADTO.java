package org.exemple.assistance_service.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReponseIADTO {
    private Long idReponseIA;
    private String diagnostic;
    private double probabilite;
    private String recommandations;
    private LocalDateTime dateGeneration;
    private String modele;
    private Long demandeId;
}