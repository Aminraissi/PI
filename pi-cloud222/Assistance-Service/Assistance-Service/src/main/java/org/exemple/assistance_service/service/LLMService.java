package org.exemple.assistance_service.service;

import org.exemple.assistance_service.dto.LLMResponseDTO;
import org.exemple.assistance_service.entity.DemandeAssistance;

public interface LLMService {
    LLMResponseDTO generateAssistanceResponse(DemandeAssistance demande);

    String getModelName();
}
