package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class VetRatingSummaryResponse {
    private Long veterinarianId;

    /** Moyenne des notes (ex: 4.3) */
    private Double moyenneNote;

    /** Nombre total d'avis */
    private long totalAvis;

    /**
     * Distribution des notes :
     * { 1: 2, 2: 1, 3: 4, 4: 8, 5: 12 }
     */
    private Map<Integer, Long> distribution;
}
