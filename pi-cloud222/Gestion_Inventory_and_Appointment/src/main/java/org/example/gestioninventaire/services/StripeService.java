package org.example.gestioninventaire.services;

import org.example.gestioninventaire.dtos.payment.StripeCheckoutRequest;
import org.example.gestioninventaire.dtos.payment.StripeCheckoutResponse;

public interface StripeService {
    StripeCheckoutResponse createCheckoutSession(StripeCheckoutRequest request);
}