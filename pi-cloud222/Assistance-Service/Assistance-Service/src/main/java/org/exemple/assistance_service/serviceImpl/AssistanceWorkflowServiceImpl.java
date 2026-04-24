package org.exemple.assistance_service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemple.assistance_service.dto.LLMResponseDTO;
import org.exemple.assistance_service.dto.ReponseIADTO;
import org.exemple.assistance_service.entity.AffectationDemande;
import org.exemple.assistance_service.entity.DemandeAssistance;
import org.exemple.assistance_service.entity.ReponseIA;
import org.exemple.assistance_service.enums.CanalTraitement;
import org.exemple.assistance_service.enums.StatutAffectation;
import org.exemple.assistance_service.enums.StatutDemande;
import org.exemple.assistance_service.exception.DuplicateAIResponseException;
import org.exemple.assistance_service.exception.ResourceNotFoundException;
import org.exemple.assistance_service.mapper.ReponseIAMapper;
import org.exemple.assistance_service.repository.AffectationDemandeRepository;
import org.exemple.assistance_service.repository.DemandeAssistanceRepository;
import org.exemple.assistance_service.repository.ReponseIARepository;
import org.exemple.assistance_service.service.AssistanceWorkflowService;
import org.exemple.assistance_service.service.ExpertRoutingService;
import org.exemple.assistance_service.service.LLMService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistanceWorkflowServiceImpl implements AssistanceWorkflowService {

    private final DemandeAssistanceRepository demandeRepository;
    private final AffectationDemandeRepository affectationDemandeRepository;
    private final ReponseIARepository reponseIARepository;
    private final ReponseIAMapper reponseIAMapper;
    private final LLMService llmService;
    private final ExpertRoutingService expertRoutingService;

    @Override
    public DemandeAssistance initializeNewDemande(DemandeAssistance demande) {
        if (shouldGenerateAI(demande)) {
            demande.setStatut(StatutDemande.EN_ATTENTE_IA);
        } else if (demande.getCanal() == CanalTraitement.INGENIEUR) {
            demande.setStatut(StatutDemande.EN_ATTENTE_INGENIEUR);
        } else if (demande.getStatut() == null) {
            demande.setStatut(StatutDemande.OUVERTE);
        }
        return demande;
    }

    @Override
    public boolean shouldGenerateAI(DemandeAssistance demande) {
        return demande.getCanal() == CanalTraitement.IA || demande.getCanal() == CanalTraitement.MIXTE;
    }

    @Override
    public boolean shouldAssignExpert(DemandeAssistance demande) {
        return demande.getCanal() == CanalTraitement.INGENIEUR || demande.getCanal() == CanalTraitement.MIXTE;
    }

    @Override
    public void assignExpertIfNeeded(DemandeAssistance demande) {
        if (!shouldAssignExpert(demande) || demande.getAffectationDemande() != null) {
            return;
        }

        Long expertId = expertRoutingService.findNextExpert(demande, Set.of());
        if (expertId == null) {
            log.warn(
                    "No configured expert found for demande id={} localisation={}; creating unassigned affectation",
                    demande.getIdDemande(),
                    demande.getLocalisation()
            );
        }

        AffectationDemande affectation = AffectationDemande.builder()
                .dateAffectation(LocalDateTime.now())
                .statut(StatutAffectation.ENVOYEE)
                .ingenieurId(expertId)
                .demandeAssistance(demande)
                .build();

        AffectationDemande saved = affectationDemandeRepository.save(affectation);
        demande.setAffectationDemande(saved);
        log.info("Demande id={} assigned to expert id={}", demande.getIdDemande(), expertId);
    }

    @Override
    @Transactional
    public ReponseIADTO generateAIResponse(Long demandeId) {
        DemandeAssistance demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande not found with id: " + demandeId));

        if (reponseIARepository.existsByDemandeAssistance_IdDemande(demandeId)) {
            throw new DuplicateAIResponseException("Demande already has an AI response: " + demandeId);
        }

        demande.setStatut(StatutDemande.EN_ATTENTE_IA);
        demandeRepository.save(demande);

        log.info("AI generation started for demande id={} model={}", demandeId, llmService.getModelName());
        LLMResponseDTO aiResponse = llmService.generateAssistanceResponse(demande);

        ReponseIA entity = ReponseIA.builder()
                .diagnostic(aiResponse.getDiagnostic())
                .probabilite(aiResponse.getProbabilite())
                .recommandations(aiResponse.getRecommandations())
                .dateGeneration(LocalDateTime.now())
                .modele(llmService.getModelName())
                .demandeAssistance(demande)
                .build();

        ReponseIA saved = reponseIARepository.save(entity);
        demande.setReponseIA(saved);
        demande.setStatut(StatutDemande.EN_COURS);
        demandeRepository.save(demande);

        log.info("AI generation finished for demande id={} reponseIA id={} model={}", demandeId, saved.getIdReponseIA(), saved.getModele());
        return reponseIAMapper.toDTO(saved);
    }
}
