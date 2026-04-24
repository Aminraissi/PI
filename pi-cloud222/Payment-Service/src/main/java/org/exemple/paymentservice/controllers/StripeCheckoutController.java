package org.exemple.paymentservice.controllers;

import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.exemple.paymentservice.dtos.StripeCheckoutRequest;
import org.exemple.paymentservice.dtos.StripeCheckoutResponse;
import org.exemple.paymentservice.services.StripeCheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stripe")
@RequiredArgsConstructor
public class StripeCheckoutController {

    private final StripeCheckoutService stripeCheckoutService;

    @PostMapping("/checkout-session")
    public ResponseEntity<StripeCheckoutResponse> createCheckoutSession(
            @RequestBody StripeCheckoutRequest request
    ) throws StripeException {
        return ResponseEntity.ok(stripeCheckoutService.createCheckoutSession(request));
    }
}