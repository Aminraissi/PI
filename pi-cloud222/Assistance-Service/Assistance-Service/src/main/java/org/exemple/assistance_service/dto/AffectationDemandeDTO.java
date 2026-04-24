package org.exemple.assistance_service.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationDemandeDTO {
    private Long idAffectation;
    private LocalDateTime dateAffectation;
    private String statut;
    private Long ingenieurId;
    private String ingenieursRefuses;
    private Long demandeId;
    private List<ReponseIngenieurDTO> reponsesIngenieur;
}
