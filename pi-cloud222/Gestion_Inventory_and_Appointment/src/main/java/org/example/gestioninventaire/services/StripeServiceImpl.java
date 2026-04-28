package org.example.gestioninventaire.services.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.example.gestioninventaire.dtos.payment.StripeCheckoutRequest;
import org.example.gestioninventaire.dtos.payment.StripeCheckoutResponse;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.services.StripeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StripeServiceImpl implements StripeService {

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public StripeCheckoutResponse createCheckoutSession(StripeCheckoutRequest request) {
        try {
            long amountInCents = Math.round(request.getMontant() * 100);

            String successUrl = frontendUrl
                    + "/appointments/payment-return?payment=success&session_id={CHECKOUT_SESSION_ID}";
            String cancelUrl = frontendUrl
                    + "/appointments/payment-return?payment=cancel";

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .putMetadata("commandeId", String.valueOf(request.getCommandeId()))
                    .putMetadata("userId",     String.valueOf(request.getUserId()))
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

            log.info("Stripe checkout session créée: {} pour commande #{}", session.getId(), request.getCommandeId());

            StripeCheckoutResponse response = new StripeCheckoutResponse();
            response.setSessionId(session.getId());
            response.setCheckoutUrl(session.getUrl());
            return response;

        } catch (StripeException e) {
            log.error("Erreur Stripe lors de la création de la session: {}", e.getMessage());
            throw new BadRequestException("Erreur Stripe: " + e.getMessage());
        }
    }
}