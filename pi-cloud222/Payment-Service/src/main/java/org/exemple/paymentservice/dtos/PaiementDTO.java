package org.exemple.paymentservice.dtos;

import lombok.*;
import org.exemple.paymentservice.enums.MethodePaiement;
import org.exemple.paymentservice.enums.StatutPaiement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * DTO for Paiement entity
 * Used for creating and updating payments
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementDTO {

    private Long idPaiement;

    @NotNull(message = "Montant cannot be null")
    @Positive(message = "Montant must be a positive value")
    private Double montant;

    private LocalDateTime datePaiement;

    @NotNull(message = "Methode Paiement cannot be null")
    private MethodePaiement methode;

    @NotNull(message = "Statut Paiement cannot be null")
    private StatutPaiement statut;

    private String reference;

    private Long factureId;

    private FactureDTO facture;

    private Long userId;

    private Long commandeId;
}

