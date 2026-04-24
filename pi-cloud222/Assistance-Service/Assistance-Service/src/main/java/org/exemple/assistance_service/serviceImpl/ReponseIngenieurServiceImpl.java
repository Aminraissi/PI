package org.exemple.assistance_service.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.exemple.assistance_service.dto.ReponseIngenieurDTO;
import org.exemple.assistance_service.entity.AffectationDemande;
import org.exemple.assistance_service.entity.ReponseIngenieur;
import org.exemple.assistance_service.enums.StatutDemande;
import org.exemple.assistance_service.enums.StatutReponse;
import org.exemple.assistance_service.mapper.ReponseIngenieurMapper;
import org.exemple.assistance_service.repository.AffectationDemandeRepository;
import org.exemple.assistance_service.repository.ReponseIngenieurRepository;
import org.exemple.assistance_service.service.ReponseIngenieurService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReponseIngenieurServiceImpl implements ReponseIngenieurService {

    private final ReponseIngenieurRepository repository;
    private final AffectationDemandeRepository affectationRepository;
    private final ReponseIngenieurMapper mapper;

    @Override
    @Transactional
    public ReponseIngenieurDTO create(Long affectationId, ReponseIngenieurDTO dto) {
        AffectationDemande affectation = affectationRepository.findById(affectationId)
                .orElseThrow(() -> new RuntimeException("Affectation not found with id: " + affectationId));

        ReponseIngenieur entity = mapper.toEntity(dto);
        entity.setDateReponse(LocalDateTime.now());
        if (entity.getStatut() == null) {
            entity.setStatut(StatutReponse.PROPOSEE);
        }
        entity.setAffectationDemande(affectation);
        if (affectation.getDemandeAssistance() != null) {
            affectation.getDemandeAssistance().setStatut(StatutDemande.RESOLUE);
        }

        return mapper.toDTO(repository.save(entity));
    }

    @Override
    public ReponseIngenieurDTO update(Long id, ReponseIngenieurDTO dto) {
        ReponseIngenieur existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ReponseIngenieur not found with id: " + id));

        existing.setContenu(dto.getContenu());
        if (dto.getStatut() != null) {
            existing.setStatut(org.exemple.assistance_service.enums.StatutReponse.valueOf(dto.getStatut()));
        }

        return mapper.toDTO(repository.save(existing));
    }

    @Override
    public ReponseIngenieurDTO getById(Long id) {
        ReponseIngenieur entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ReponseIngenieur not found with id: " + id));
        return mapper.toDTO(entity);
    }

    @Override
    public List<ReponseIngenieurDTO> getAll() {
        return repository.findAll().stream().map(mapper::toDTO).toList();
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
