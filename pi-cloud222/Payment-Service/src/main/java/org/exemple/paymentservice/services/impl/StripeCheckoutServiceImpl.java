package org.exemple.paymentservice.services.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.exemple.paymentservice.dtos.StripeCheckoutRequest;
import org.exemple.paymentservice.dtos.StripeCheckoutResponse;
import org.exemple.paymentservice.services.StripeCheckoutService;
import org.springframework.stereotype.Service;

@Service
public class StripeCheckoutServiceImpl implements StripeCheckoutService {

    @Override
    public StripeCheckoutResponse createCheckoutSession(StripeCheckoutRequest request) throws StripeException {
        long amountInCents = Math.round(request.getMontant() * 100);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:4200/marketplace/cart?payment=success&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("http://localhost:4200/marketplace/cart?payment=cancel")
                .putMetadata("commandeId", String.valueOf(request.getCommandeId()))
                .putMetadata("userId", String.valueOf(request.getUserId()))
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(
                                                                        request.getProductName() != null && !request.getProductName().isBlank()
                                                                                ? request.getProductName()
                                                                                : "Commande GreenRoots #" + request.getCommandeId()
                                                                )
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        return new StripeCheckoutResponse(session.getId(), session.getUrl());
    }
}