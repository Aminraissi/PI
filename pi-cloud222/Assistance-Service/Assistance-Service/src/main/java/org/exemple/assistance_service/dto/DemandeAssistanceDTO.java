package org.exemple.assistance_service.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeAssistanceDTO {
    private Long idDemande;
    private String typeProbleme;
    private String description;
    private String mediaUrl;
    private String localisation;
    private LocalDateTime dateCreation;
    private String canal;
    private String statut;
    private Long userId;
    private ReponseIADTO reponseIA;
    private AffectationDemandeDTO affectationDemande;
}
