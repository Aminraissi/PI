package org.exemple.assistance_service.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.exemple.assistance_service.dto.AffectationDemandeDTO;
import org.exemple.assistance_service.entity.AffectationDemande;
import org.exemple.assistance_service.entity.DemandeAssistance;
import org.exemple.assistance_service.enums.StatutAffectation;
import org.exemple.assistance_service.enums.StatutDemande;
import org.exemple.assistance_service.exception.ResourceNotFoundException;
import org.exemple.assistance_service.mapper.AffectationDemandeMapper;
import org.exemple.assistance_service.repository.AffectationDemandeRepository;
import org.exemple.assistance_service.repository.DemandeAssistanceRepository;
import org.exemple.assistance_service.service.AffectationDemandeService;
import org.exemple.assistance_service.service.ExpertRoutingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AffectationDemandeServiceImpl implements AffectationDemandeService {

    private final AffectationDemandeRepository repository;
    private final DemandeAssistanceRepository demandeRepository;
    private final AffectationDemandeMapper mapper;
    private final ExpertRoutingService expertRoutingService;

    @Override
    public AffectationDemandeDTO create(Long demandeId, AffectationDemandeDTO dto) {
        DemandeAssistance demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande not found with id: " + demandeId));

        AffectationDemande entity = mapper.toEntity(dto);
        entity.setDateAffectation(LocalDateTime.now());
        if (entity.getStatut() == null) {
            entity.setStatut(StatutAffectation.ENVOYEE);
        }
        entity.setDemandeAssistance(demande);

        return mapper.toDTO(repository.save(entity));
    }

    @Override
    public AffectationDemandeDTO update(Long id, AffectationDemandeDTO dto) {
        AffectationDemande existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Affectation not found with id: " + id));

        if (dto.getStatut() != null) {
            existing.setStatut(StatutAffectation.valueOf(dto.getStatut()));
        }
        existing.setIngenieurId(dto.getIngenieurId());

        return mapper.toDTO(repository.save(existing));
    }

    @Override
    public AffectationDemandeDTO getById(Long id) {
        AffectationDemande entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Affectation not found with id: " + id));
        return mapper.toDTO(entity);
    }

    @Override
    public List<AffectationDemandeDTO> getAll() {
        return repository.findAll().stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<AffectationDemandeDTO> getByIngenieurId(Long ingenieurId) {
        return repository.findByIngenieurId(ingenieurId).stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<AffectationDemandeDTO> getPendingByIngenieurId(Long ingenieurId) {
        return repository.findByIngenieurIdAndStatut(ingenieurId, StatutAffectation.ENVOYEE)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public AffectationDemandeDTO accept(Long id, Long ingenieurId) {
        AffectationDemande affectation = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Affectation not found with id: " + id));
        assignIfUnassigned(affectation, ingenieurId);
        validateAssignedExpert(affectation, ingenieurId);

        affectation.setStatut(StatutAffectation.ACCEPTEE);
        if (affectation.getDemandeAssistance() != null) {
            affectation.getDemandeAssistance().setStatut(StatutDemande.EN_COURS);
        }

        return mapper.toDTO(repository.save(affectation));
    }

    @Override
    public AffectationDemandeDTO refuse(Long id, Long ingenieurId) {
        AffectationDemande affectation = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Affectation not found with id: " + id));
        validateAssignedExpert(affectation, ingenieurId);

        Set<Long> refusedIds = parseRefusedIds(affectation.getIngenieursRefuses());
        refusedIds.add(ingenieurId);
        Long nextExpertId = expertRoutingService.findNextExpert(affectation.getDemandeAssistance(), refusedIds);

        affectation.setIngenieursRefuses(formatRefusedIds(refusedIds));
        affectation.setDateAffectation(LocalDateTime.now());

        if (nextExpertId == null) {
            affectation.setStatut(StatutAffectation.REFUSEE);
            affectation.setIngenieurId(null);
            if (affectation.getDemandeAssistance() != null) {
                affectation.getDemandeAssistance().setStatut(StatutDemande.EN_ATTENTE_INGENIEUR);
            }
        } else {
            affectation.setStatut(StatutAffectation.ENVOYEE);
            affectation.setIngenieurId(nextExpertId);
        }

        return mapper.toDTO(repository.save(affectation));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void validateAssignedExpert(AffectationDemande affectation, Long ingenieurId) {
        if (ingenieurId == null || !ingenieurId.equals(affectation.getIngenieurId())) {
            throw new IllegalArgumentException("This affectation is not assigned to expert id: " + ingenieurId);
        }
    }

    private void assignIfUnassigned(AffectationDemande affectation, Long ingenieurId) {
        if (affectation.getIngenieurId() == null && ingenieurId != null) {
            affectation.setIngenieurId(ingenieurId);
        }
    }

    private Set<Long> parseRefusedIds(String rawIds) {
        if (rawIds == null || rawIds.isBlank()) {
            return new LinkedHashSet<>();
        }

        Set<Long> ids = new LinkedHashSet<>();
        Arrays.stream(rawIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(value -> {
                    try {
                        ids.add(Long.valueOf(value));
                    } catch (NumberFormatException ignored) {
                        // Ignore bad persisted values and keep routing.
                    }
                });
        return ids;
    }

    private String formatRefusedIds(Set<Long> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
