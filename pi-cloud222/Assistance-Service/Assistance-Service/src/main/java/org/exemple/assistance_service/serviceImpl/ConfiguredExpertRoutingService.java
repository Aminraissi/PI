package org.exemple.assistance_service.serviceImpl;

import org.exemple.assistance_service.entity.DemandeAssistance;
import org.exemple.assistance_service.service.ExpertRoutingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ConfiguredExpertRoutingService implements ExpertRoutingService {

    @Value("${assistance.expert-routing.region-experts:}")
    private String regionExperts;

    @Value("${assistance.expert-routing.default-experts:}")
    private String defaultExperts;

    @Override
    public Long findNextExpert(DemandeAssistance demande, Set<Long> excludedExpertIds) {
        List<Long> candidates = findCandidatesForRegion(demande.getLocalisation());
        if (candidates.isEmpty()) {
            candidates = parseExpertIds(defaultExperts);
        }

        return candidates.stream()
                .filter(id -> !excludedExpertIds.contains(id))
                .findFirst()
                .orElse(null);
    }

    private List<Long> findCandidatesForRegion(String localisation) {
        if (regionExperts == null || regionExperts.isBlank() || localisation == null || localisation.isBlank()) {
            return List.of();
        }

        String normalizedLocation = normalize(localisation);
        for (String entry : regionExperts.split(";")) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            String region = normalize(parts[0]);
            if (!region.isBlank() && normalizedLocation.contains(region)) {
                return parseExpertIds(parts[1]);
            }
        }
        return List.of();
    }

    private List<Long> parseExpertIds(String rawIds) {
        if (rawIds == null || rawIds.isBlank()) {
            return List.of();
        }

        List<Long> ids = new ArrayList<>();
        for (String rawId : rawIds.split(",")) {
            try {
                ids.add(Long.valueOf(rawId.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore bad config entries and keep routing with the valid ids.
            }
        }
        return ids;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
