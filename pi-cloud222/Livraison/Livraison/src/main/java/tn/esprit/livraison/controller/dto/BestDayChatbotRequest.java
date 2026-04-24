package tn.esprit.livraison.controller.dto;

import java.time.LocalDate;

public record BestDayChatbotRequest(
        Integer farmerId,
        String message,
        Double pickupLat,
        Double pickupLng,
        LocalDate fromDate,
        LocalDate toDate
) {
}
