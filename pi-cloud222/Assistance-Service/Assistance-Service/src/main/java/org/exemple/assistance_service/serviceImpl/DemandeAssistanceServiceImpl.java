package org.exemple.assistance_service.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.exemple.assistance_service.dto.DemandeAssistanceDTO;
import org.exemple.assistance_service.entity.DemandeAssistance;
import org.exemple.assistance_service.exception.LLMException;
import org.exemple.assistance_service.mapper.DemandeAssistanceMapper;
import org.exemple.assistance_service.repository.DemandeAssistanceRepository;
import org.exemple.assistance_service.service.AssistanceWorkflowService;
import org.exemple.assistance_service.service.DemandeAssistanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DemandeAssistanceServiceImpl implements DemandeAssistanceService {

    private static final Logger log = LoggerFactory.getLogger(DemandeAssistanceServiceImpl.class);

    private final DemandeAssistanceRepository repository;
    private final DemandeAssistanceMapper mapper;
    private final AssistanceWorkflowService workflowService;

    @Override
    public DemandeAssistanceDTO create(DemandeAssistanceDTO dto) {
        DemandeAssistance entity = mapper.toEntity(dto);
        entity.setDateCreation(LocalDateTime.now());
        workflowService.initializeNewDemande(entity);

        DemandeAssistance saved = repository.save(entity);
        workflowService.assignExpertIfNeeded(saved);
        saved = repository.findById(saved.getIdDemande()).orElse(saved);

        if (workflowService.shouldGenerateAI(saved)) {
            try {
                workflowService.generateAIResponse(saved.getIdDemande());
                saved = repository.findById(saved.getIdDemande()).orElse(saved);
            } catch (LLMException ex) {
                log.error("Automatic AI generation failed for demande id={}", saved.getIdDemande(), ex);
            }
        }
        return mapper.toDTO(saved);
    }

    @Override
    public DemandeAssistanceDTO update(Long id, DemandeAssistanceDTO dto) {
        DemandeAssistance existing = repository.findById(id)
                .orElseThrow(() -> new org.exemple.assistance_service.exception.ResourceNotFoundException("Demande not found with id: " + id));

        existing.setTypeProbleme(dto.getTypeProbleme() != null
                ? org.exemple.assistance_service.enums.TypeProbleme.valueOf(dto.getTypeProbleme())
                : existing.getTypeProbleme());
        existing.setDescription(dto.getDescription());
        existing.setMediaUrl(dto.getMediaUrl());
        existing.setLocalisation(dto.getLocalisation());
        existing.setCanal(dto.getCanal() != null
                ? org.exemple.assistance_service.enums.CanalTraitement.valueOf(dto.getCanal())
                : existing.getCanal());
        existing.setStatut(dto.getStatut() != null
                ? org.exemple.assistance_service.enums.StatutDemande.valueOf(dto.getStatut())
                : existing.getStatut());
        existing.setUserId(dto.getUserId());

        return mapper.toDTO(repository.save(existing));
    }

    @Override
    public DemandeAssistanceDTO getById(Long id) {
        return mapper.toDTO(repository.findById(id)
                .orElseThrow(() -> new org.exemple.assistance_service.exception.ResourceNotFoundException("Demande not found with id: " + id)));
    }

    @Override
    public List<DemandeAssistanceDTO> getAll() {
        return repository.findAll().stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<DemandeAssistanceDTO> getByUserId(Long userId) {
        return repository.findByUserId(userId).stream().map(mapper::toDTO).toList();
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
