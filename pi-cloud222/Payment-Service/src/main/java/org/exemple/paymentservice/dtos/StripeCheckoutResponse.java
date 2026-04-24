package org.exemple.paymentservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StripeCheckoutResponse {
    private String sessionId;
    private String checkoutUrl;
}