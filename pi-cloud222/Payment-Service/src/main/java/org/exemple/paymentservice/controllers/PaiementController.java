package org.exemple.paymentservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemple.paymentservice.dtos.PaiementDTO;
import org.exemple.paymentservice.services.PaiementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * REST Controller for Paiement (Payment) API
 */
@RestController
@RequestMapping("/api/v1/paiements")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PaiementController {

    private final PaiementService paiementService;

    /**
     * Get all payments
     * @return list of payments
     */
    @GetMapping
    public ResponseEntity<List<PaiementDTO>> getAllPaiements() {
        log.info("GET /api/v1/paiements - Get all payments");
        List<PaiementDTO> paiements = paiementService.getAllPaiements();
        return ResponseEntity.ok(paiements);
    }

    /**
     * Get payment by id
     * @param id payment id
     * @return payment details
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaiementDTO> getPaiementById(
            @PathVariable @Positive(message = "ID must be positive") Long id) {
        log.info("GET /api/v1/paiements/{} - Get payment by id", id);
        PaiementDTO paiement = paiementService.getPaiementById(id);
        return ResponseEntity.ok(paiement);
    }

    /**
     * Get payment by reference
     * @param reference payment reference
     * @return payment details
     */
    @GetMapping("/reference/{reference}")
    public ResponseEntity<PaiementDTO> getPaiementByReference(
            @PathVariable String reference) {
        log.info("GET /api/v1/paiements/reference/{} - Get payment by reference", reference);
        PaiementDTO paiement = paiementService.getPaiementByReference(reference);
        return ResponseEntity.ok(paiement);
    }

    /**
     * Get payment by facture id
     * @param factureId facture id
     * @return payment details
     */
    @GetMapping("/facture/{factureId}")
    public ResponseEntity<PaiementDTO> getPaiementByFactureId(
            @PathVariable @Positive(message = "Facture ID must be positive") Long factureId) {
        log.info("GET /api/v1/paiements/facture/{} - Get payment by facture id", factureId);
        PaiementDTO paiement = paiementService.getPaiementByFactureId(factureId);
        return ResponseEntity.ok(paiement);
    }

    /**
     * Get all payments for a facture
     * @param factureId facture id
     * @return list of payments
     */
    @GetMapping("/facture/{factureId}/all")
    public ResponseEntity<List<PaiementDTO>> getPaiementsByFactureId(
            @PathVariable @Positive(message = "Facture ID must be positive") Long factureId) {
        log.info("GET /api/v1/paiements/facture/{}/all - Get all payments for facture", factureId);
        List<PaiementDTO> paiements = paiementService.getPaiementsByFactureId(factureId);
        return ResponseEntity.ok(paiements);
    }

    /**
     * Create a new payment with automatic facture generation
     * This endpoint automatically creates a new Facture linked to the payment
     * @param paiementDTO payment data (montant, methode, statut are required)
     * @return created payment with facture information
     */
    @PostMapping
    public ResponseEntity<PaiementDTO> createPaiement(@Valid @RequestBody PaiementDTO paiementDTO) {
        log.info("POST /api/v1/paiements - Create payment with automatic facture");
        PaiementDTO created = paiementService.createPaiement(paiementDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Create a new payment for an existing facture
     * @param factureId facture id
     * @param paiementDTO payment data
     * @return created payment
     */
    @PostMapping("/facture/{factureId}")
    public ResponseEntity<PaiementDTO> createPaiementForFacture(
            @PathVariable @Positive(message = "Facture ID must be positive") Long factureId,
            @Valid @RequestBody PaiementDTO paiementDTO) {
        log.info("POST /api/v1/paiements/facture/{} - Create payment for facture", factureId);
        PaiementDTO created = paiementService.createPaiement(factureId, paiementDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update a payment
     * @param id payment id
     * @param paiementDTO payment data
     * @return updated payment
     */
    @PutMapping("/{id}")
    public ResponseEntity<PaiementDTO> updatePaiement(
            @PathVariable @Positive(message = "ID must be positive") Long id,
            @Valid @RequestBody PaiementDTO paiementDTO) {
        log.info("PUT /api/v1/paiements/{} - Update payment", id);
        PaiementDTO updated = paiementService.updatePaiement(id, paiementDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a payment
     * @param id payment id
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaiement(
            @PathVariable @Positive(message = "ID must be positive") Long id) {
        log.info("DELETE /api/v1/paiements/{} - Delete payment", id);
        paiementService.deletePaiement(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}/paid")
    public ResponseEntity<List<PaiementDTO>> getPaidPaiementsByUser(
            @PathVariable Long userId
    ) {
        log.info("GET /api/v1/paiements/user/{}/paid - Get paid payments by user", userId);
        List<PaiementDTO> paiements = paiementService.getPaidPaiementsByUser(userId);
        return ResponseEntity.ok(paiements);
    }
}

