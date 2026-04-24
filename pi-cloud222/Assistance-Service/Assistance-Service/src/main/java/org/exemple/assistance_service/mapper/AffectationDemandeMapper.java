package org.exemple.assistance_service.mapper;

import org.exemple.assistance_service.dto.AffectationDemandeDTO;
import org.exemple.assistance_service.entity.AffectationDemande;
import org.exemple.assistance_service.enums.StatutAffectation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AffectationDemandeMapper {

    private final ReponseIngenieurMapper reponseIngenieurMapper;

    public AffectationDemandeMapper(ReponseIngenieurMapper reponseIngenieurMapper) {
        this.reponseIngenieurMapper = reponseIngenieurMapper;
    }

    public AffectationDemandeDTO toDTO(AffectationDemande entity) {
        return AffectationDemandeDTO.builder()
                .idAffectation(entity.getIdAffectation())
                .dateAffectation(entity.getDateAffectation())
                .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
                .ingenieurId(entity.getIngenieurId())
                .ingenieursRefuses(entity.getIngenieursRefuses())
                .demandeId(entity.getDemandeAssistance() != null ? entity.getDemandeAssistance().getIdDemande() : null)
                .reponsesIngenieur(entity.getReponsesIngenieur() != null
                        ? entity.getReponsesIngenieur().stream().map(reponseIngenieurMapper::toDTO).toList()
                        : List.of())
                .build();
    }

    public AffectationDemande toEntity(AffectationDemandeDTO dto) {
        return AffectationDemande.builder()
                .idAffectation(dto.getIdAffectation())
                .dateAffectation(dto.getDateAffectation())
                .statut(dto.getStatut() != null ? StatutAffectation.valueOf(dto.getStatut()) : null)
                .ingenieurId(dto.getIngenieurId())
                .ingenieursRefuses(dto.getIngenieursRefuses())
                .build();
    }
}
