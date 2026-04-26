package org.example.gestioninventaire.dtos.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StripeCheckoutRequest {
    private Long commandeId;
    private Long userId;
    private Double montant;
    private String productName;
}

