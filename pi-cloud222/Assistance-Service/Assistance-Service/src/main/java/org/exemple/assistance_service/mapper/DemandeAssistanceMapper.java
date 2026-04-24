package org.exemple.assistance_service.mapper;

import org.exemple.assistance_service.dto.DemandeAssistanceDTO;
import org.exemple.assistance_service.entity.DemandeAssistance;
import org.exemple.assistance_service.enums.CanalTraitement;
import org.exemple.assistance_service.enums.StatutDemande;
import org.exemple.assistance_service.enums.TypeProbleme;
import org.springframework.stereotype.Component;

@Component
public class DemandeAssistanceMapper {

    private final ReponseIAMapper reponseIAMapper;
    private final AffectationDemandeMapper affectationDemandeMapper;

    public DemandeAssistanceMapper(ReponseIAMapper reponseIAMapper, AffectationDemandeMapper affectationDemandeMapper) {
        this.reponseIAMapper = reponseIAMapper;
        this.affectationDemandeMapper = affectationDemandeMapper;
    }

    public DemandeAssistanceDTO toDTO(DemandeAssistance entity) {
        return DemandeAssistanceDTO.builder()
                .idDemande(entity.getIdDemande())
                .typeProbleme(entity.getTypeProbleme() != null ? entity.getTypeProbleme().name() : null)
                .description(entity.getDescription())
                .mediaUrl(entity.getMediaUrl())
                .localisation(entity.getLocalisation())
                .dateCreation(entity.getDateCreation())
                .canal(entity.getCanal() != null ? entity.getCanal().name() : null)
                .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
                .userId(entity.getUserId())
                .reponseIA(entity.getReponseIA() != null ? reponseIAMapper.toDTO(entity.getReponseIA()) : null)
                .affectationDemande(entity.getAffectationDemande() != null ? affectationDemandeMapper.toDTO(entity.getAffectationDemande()) : null)
                .build();
    }

    public DemandeAssistance toEntity(DemandeAssistanceDTO dto) {
        return DemandeAssistance.builder()
                .idDemande(dto.getIdDemande())
                .typeProbleme(dto.getTypeProbleme() != null ? TypeProbleme.valueOf(dto.getTypeProbleme()) : null)
                .description(dto.getDescription())
                .mediaUrl(dto.getMediaUrl())
                .localisation(dto.getLocalisation())
                .dateCreation(dto.getDateCreation())
                .canal(dto.getCanal() != null ? CanalTraitement.valueOf(dto.getCanal()) : null)
                .statut(dto.getStatut() != null ? StatutDemande.valueOf(dto.getStatut()) : null)
                .userId(dto.getUserId())
                .build();
    }
}
