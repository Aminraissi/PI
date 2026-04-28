package org.example.gestioninventaire.feigns;

import org.example.gestioninventaire.dtos.payment.StripeCheckoutRequest;
import org.example.gestioninventaire.dtos.payment.StripeCheckoutResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/api/v1/stripe/checkout-session")
    StripeCheckoutResponse createCheckoutSession(@RequestBody StripeCheckoutRequest request);
}
