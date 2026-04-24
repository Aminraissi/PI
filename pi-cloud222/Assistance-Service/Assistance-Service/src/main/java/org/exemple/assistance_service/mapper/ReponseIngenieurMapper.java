package org.exemple.assistance_service.mapper;

import org.exemple.assistance_service.dto.ReponseIngenieurDTO;
import org.exemple.assistance_service.entity.ReponseIngenieur;
import org.exemple.assistance_service.enums.StatutReponse;
import org.springframework.stereotype.Component;

@Component
public class ReponseIngenieurMapper {

    public ReponseIngenieurDTO toDTO(ReponseIngenieur entity) {
        return ReponseIngenieurDTO.builder()
                .idReponse(entity.getIdReponse())
                .contenu(entity.getContenu())
                .dateReponse(entity.getDateReponse())
                .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
                .affectationId(entity.getAffectationDemande() != null ? entity.getAffectationDemande().getIdAffectation() : null)
                .build();
    }

    public ReponseIngenieur toEntity(ReponseIngenieurDTO dto) {
        return ReponseIngenieur.builder()
                .idReponse(dto.getIdReponse())
                .contenu(dto.getContenu())
                .dateReponse(dto.getDateReponse())
                .statut(dto.getStatut() != null ? StatutReponse.valueOf(dto.getStatut()) : null)
                .build();
    }
}