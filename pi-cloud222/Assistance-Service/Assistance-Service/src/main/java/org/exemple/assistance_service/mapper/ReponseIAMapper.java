package org.exemple.assistance_service.mapper;

import org.exemple.assistance_service.dto.ReponseIADTO;
import org.exemple.assistance_service.entity.ReponseIA;
import org.springframework.stereotype.Component;

@Component
public class ReponseIAMapper {

    public ReponseIADTO toDTO(ReponseIA entity) {
        return ReponseIADTO.builder()
                .idReponseIA(entity.getIdReponseIA())
                .diagnostic(entity.getDiagnostic())
                .probabilite(entity.getProbabilite())
                .recommandations(entity.getRecommandations())
                .dateGeneration(entity.getDateGeneration())
                .modele(entity.getModele())
                .demandeId(entity.getDemandeAssistance() != null ? entity.getDemandeAssistance().getIdDemande() : null)
                .build();
    }

    public ReponseIA toEntity(ReponseIADTO dto) {
        return ReponseIA.builder()
                .idReponseIA(dto.getIdReponseIA())
                .diagnostic(dto.getDiagnostic())
                .probabilite(dto.getProbabilite())
                .recommandations(dto.getRecommandations())
                .dateGeneration(dto.getDateGeneration())
                .modele(dto.getModele())
                .build();
    }
}