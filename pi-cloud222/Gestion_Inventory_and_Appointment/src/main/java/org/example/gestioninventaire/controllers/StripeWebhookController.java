package org.example.gestioninventaire.controllers;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gestioninventaire.services.CommandeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final CommandeService commandeService;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    /**
     * Webhook Stripe — appelé automatiquement par Stripe après un paiement.
     * Configure l'URL dans le dashboard Stripe :
     *   http://localhost:8088/inventaires/api/stripe/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader
    ) {
        Event event;

        try {
            if (webhookSecret != null && !webhookSecret.isBlank() && sigHeader != null) {
                event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            } else {
                // Mode développement sans vérification de signature
                log.warn("Webhook reçu sans vérification de signature (mode dev)");
                event = com.stripe.model.Event.PRETTY_PRINT_GSON
                        .fromJson(payload, Event.class);
            }
        } catch (SignatureVerificationException e) {
            log.error("Signature Stripe invalide: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Payload Stripe invalide: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        try {
            if ("checkout.session.completed".equals(event.getType())) {
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                Session session;
                try {
                    session = (Session) deserializer.deserializeUnsafe();
                } catch (Exception e) {
                    log.error("Impossible de désérialiser la session Stripe: {}", e.getMessage());
                    return ResponseEntity.ok("Cannot deserialize session");
                }

                if (session == null) {
                    return ResponseEntity.ok("Session null");
                }

                String commandeIdStr = session.getMetadata() != null
                        ? session.getMetadata().get("commandeId") : null;

                if (commandeIdStr == null) {
                    log.warn("Metadata commandeId manquante dans la session Stripe {}", session.getId());
                    return ResponseEntity.ok("Metadata missing");
                }

                Long commandeId = Long.valueOf(commandeIdStr);
                log.info("Webhook Stripe: paiement confirmé pour commande #{}", commandeId);
                commandeService.confirmerPaiementCommande(commandeId);
            }

            return ResponseEntity.ok("Webhook traité");

        } catch (Exception e) {
            log.error("Erreur traitement webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }
}