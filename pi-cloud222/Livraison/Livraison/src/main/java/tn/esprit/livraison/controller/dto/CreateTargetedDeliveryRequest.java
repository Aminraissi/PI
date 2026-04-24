package tn.esprit.livraison.controller.dto;

import java.time.LocalDateTime;

public record CreateTargetedDeliveryRequest(
        Integer transporteurId,
        String reference,
        String product,
        Double weightKg,
        String details,
        LocalDateTime departureDate,
        String pickupLabel,
        String dropoffLabel,
        Double estimatedPrice,
        Double pickupLat,
        Double pickupLng,
        Double dropoffLat,
        Double dropoffLng,
        Boolean autoGrouping
) {
}
