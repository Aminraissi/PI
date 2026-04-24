package org.exemple.paymentservice.controllers;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.SetupIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.exemple.paymentservice.dtos.PaiementDTO;
import org.exemple.paymentservice.enums.MethodePaiement;
import org.exemple.paymentservice.enums.StatutPaiement;
import org.exemple.paymentservice.services.PaiementService;
import org.exemple.paymentservice.services.RentalPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/paiements")
public class StripeWebhookController {

    @Autowired
    private PaiementService paiementService;

    @Autowired
    private RentalPaymentService rentalPaymentService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            System.out.println("Webhook event reçu: " + event.getType());
        } catch (SignatureVerificationException e) {
            System.out.println("Signature Stripe invalide");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            System.out.println("Payload Stripe invalide: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        try {
            if ("checkout.session.completed".equals(event.getType())) {
                System.out.println("Event checkout.session.completed détecté");

                EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

                Session session;

                try {
                    session = (Session) dataObjectDeserializer.deserializeUnsafe();
                } catch (Exception e) {
                    System.out.println("Could not deserialize Stripe session: " + e.getMessage());
                    return ResponseEntity.ok("Could not deserialize session");
                }

                if (session == null) {
                    System.out.println("Session Stripe is null");
                    return ResponseEntity.ok("Session null");
                }

                System.out.println("Session Stripe id = " + session.getId());
                System.out.println("Session mode = " + session.getMode());
                System.out.println("Session customer = " + session.getCustomer());
                System.out.println("Session setupIntent = " + session.getSetupIntent());
                System.out.println("Session metadata = " + session.getMetadata());

                String type = session.getMetadata() != null
                        ? session.getMetadata().get("type")
                        : null;

                if ("RENTAL_SETUP".equals(type)) {
                    String propositionIdStr = session.getMetadata().get("propositionId");

                    if (propositionIdStr == null) {
                        return ResponseEntity.ok("Rental setup metadata missing");
                    }

                    Long propositionId = Long.valueOf(propositionIdStr);

                    String customerId = session.getCustomer();
                    String setupIntentId = session.getSetupIntent();

                    SetupIntent setupIntent = SetupIntent.retrieve(setupIntentId);
                    String paymentMethodId = setupIntent.getPaymentMethod();

                    rentalPaymentService.activateAutoPayment(
                            propositionId,
                            customerId,
                            paymentMethodId
                    );

                    System.out.println("Rental auto payment activated for proposition " + propositionId);
                    System.out.println("Customer = " + customerId);
                    System.out.println("Payment method = " + paymentMethodId);

                    return ResponseEntity.ok("Rental setup completed");
                }

                String commandeIdStr = session.getMetadata() != null ? session.getMetadata().get("commandeId") : null;
                String userIdStr = session.getMetadata() != null ? session.getMetadata().get("userId") : null;

                System.out.println("commandeId metadata = " + commandeIdStr);
                System.out.println("userId metadata = " + userIdStr);

                if (commandeIdStr == null || userIdStr == null) {
                    System.out.println("Metadata manquante dans la session Stripe");
                    return ResponseEntity.ok("Metadata missing");
                }

                Long commandeId = Long.valueOf(commandeIdStr);
                Long userId = Long.valueOf(userIdStr);

                Double montant = session.getAmountTotal() != null
                        ? session.getAmountTotal() / 100.0
                        : 0.0;

                PaiementDTO dto = new PaiementDTO();
                dto.setMontant(montant);
                dto.setDatePaiement(LocalDateTime.now());
                dto.setMethode(MethodePaiement.CARTE);
                dto.setStatut(StatutPaiement.PAYE);
                dto.setUserId(userId);
                dto.setCommandeId(commandeId);

                System.out.println("Avant createPaiement()");
                PaiementDTO savedPaiement = paiementService.createPaiement(dto);
                System.out.println("Paiement enregistré avec id = " + savedPaiement.getIdPaiement());

                String venteUrl = "http://localhost:8089/Vente/api/commande/validate/" + commandeId;
                System.out.println("Appel microservice vente: " + venteUrl);

                ResponseEntity<String> venteResponse =
                        restTemplate.exchange(venteUrl, HttpMethod.PUT, null, String.class);

                System.out.println("Réponse microservice vente = " + venteResponse.getStatusCode());
            }

            return ResponseEntity.ok("Webhook reçu");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur webhook: " + e.getMessage());
        }
    }
}