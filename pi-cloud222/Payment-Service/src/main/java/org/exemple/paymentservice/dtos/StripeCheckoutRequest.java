package org.exemple.paymentservice.dtos;

import lombok.Data;

@Data
public class StripeCheckoutRequest {
    private Long commandeId;
    private Long userId;
    private Double montant;
    private String productName;
}