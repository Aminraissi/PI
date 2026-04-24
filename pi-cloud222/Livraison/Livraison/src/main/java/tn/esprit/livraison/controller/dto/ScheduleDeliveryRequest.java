package tn.esprit.livraison.controller.dto;

import java.time.LocalDateTime;

public record ScheduleDeliveryRequest(
        Integer agriculteurId,
        LocalDateTime dateLivraisonPrevue
) {
}

