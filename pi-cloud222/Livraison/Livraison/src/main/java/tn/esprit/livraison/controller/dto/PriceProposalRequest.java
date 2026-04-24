package tn.esprit.livraison.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDateTime;

public record PriceProposalRequest(
        Integer actorId,
        Double prixPropose,
        @JsonAlias({"dateProposeeNegociation", "proposedDate", "dateProposee"})
        LocalDateTime proposedDateTime
) {
}
