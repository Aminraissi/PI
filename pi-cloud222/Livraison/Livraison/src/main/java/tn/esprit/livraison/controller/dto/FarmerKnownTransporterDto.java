package tn.esprit.livraison.controller.dto;

import java.time.LocalDateTime;

public record FarmerKnownTransporterDto(
        Integer transporteurId,
        String displayName,
        long completedDeliveries,
        LocalDateTime lastDeliveryAt
) {
}
