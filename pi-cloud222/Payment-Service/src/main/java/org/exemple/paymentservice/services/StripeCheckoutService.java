package org.exemple.paymentservice.services;

import com.stripe.exception.StripeException;
import org.exemple.paymentservice.dtos.StripeCheckoutRequest;
import org.exemple.paymentservice.dtos.StripeCheckoutResponse;

public interface StripeCheckoutService {
    StripeCheckoutResponse createCheckoutSession(StripeCheckoutRequest request) throws StripeException;
}