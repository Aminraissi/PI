package org.exemple.assistance_service.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.exemple.assistance_service.dto.ReponseIADTO;
import org.exemple.assistance_service.entity.DemandeAssistance;
import org.exemple.assistance_service.entity.ReponseIA;
import org.exemple.assistance_service.exception.DuplicateAIResponseException;
import org.exemple.assistance_service.exception.ResourceNotFoundException;
import org.exemple.assistance_service.mapper.ReponseIAMapper;
import org.exemple.assistance_service.repository.DemandeAssistanceRepository;
import org.exemple.assistance_service.repository.ReponseIARepository;
import org.exemple.assistance_service.service.ReponseIAService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReponseIAServiceImpl implements ReponseIAService {

    private final ReponseIARepository reponseIARepository;
    private final DemandeAssistanceRepository demandeRepository;
    private final ReponseIAMapper mapper;

    @Override
    public ReponseIADTO create(Long demandeId, ReponseIADTO dto) {
        DemandeAssistance demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande not found with id: " + demandeId));

        if (reponseIARepository.existsByDemandeAssistance_IdDemande(demandeId)) {
            throw new DuplicateAIResponseException("Demande already has an AI response: " + demandeId);
        }

        ReponseIA entity = mapper.toEntity(dto);
        entity.setDateGeneration(LocalDateTime.now());
        entity.setDemandeAssistance(demande);

        return mapper.toDTO(reponseIARepository.save(entity));
    }

    @Override
    public ReponseIADTO update(Long id, ReponseIADTO dto) {
        ReponseIA existing = reponseIARepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReponseIA not found with id: " + id));

        existing.setDiagnostic(dto.getDiagnostic());
        existing.setProbabilite(dto.getProbabilite());
        existing.setRecommandations(dto.getRecommandations());
        existing.setModele(dto.getModele());

        return mapper.toDTO(reponseIARepository.save(existing));
    }

    @Override
    public ReponseIADTO getById(Long id) {
        ReponseIA entity = reponseIARepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReponseIA not found with id: " + id));
        return mapper.toDTO(entity);
    }

    @Override
    public List<ReponseIADTO> getAll() {
        return reponseIARepository.findAll().stream().map(mapper::toDTO).toList();
    }

    @Override
    public void delete(Long id) {
        reponseIARepository.deleteById(id);
    }
}
