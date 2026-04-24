package org.exemple.assistance_service.service;

import org.exemple.assistance_service.dto.ReponseIADTO;
import org.exemple.assistance_service.entity.DemandeAssistance;

public interface AssistanceWorkflowService {
    DemandeAssistance initializeNewDemande(DemandeAssistance demande);

    boolean shouldGenerateAI(DemandeAssistance demande);

    boolean shouldAssignExpert(DemandeAssistance demande);

    void assignExpertIfNeeded(DemandeAssistance demande);

    ReponseIADTO generateAIResponse(Long demandeId);
}
