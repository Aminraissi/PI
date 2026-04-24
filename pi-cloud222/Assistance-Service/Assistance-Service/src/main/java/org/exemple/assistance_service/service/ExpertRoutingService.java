package org.exemple.assistance_service.service;

import org.exemple.assistance_service.entity.DemandeAssistance;

import java.util.Set;

public interface ExpertRoutingService {
    Long findNextExpert(DemandeAssistance demande, Set<Long> excludedExpertIds);
}
