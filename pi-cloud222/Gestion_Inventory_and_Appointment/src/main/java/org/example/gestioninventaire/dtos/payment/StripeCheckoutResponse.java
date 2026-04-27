package org.example.gestioninventaire.dtos.payment;

import lombok.Data;

@Data
public class StripeCheckoutResponse {
    private String sessionId;
    private String checkoutUrl;
}

