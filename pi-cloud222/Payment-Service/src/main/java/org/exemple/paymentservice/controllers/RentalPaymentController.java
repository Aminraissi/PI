package org.exemple.paymentservice.controllers;

import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.exemple.paymentservice.dtos.CreateRentalPaymentPlanRequest;
import org.exemple.paymentservice.entities.PaiementLocation;
import org.exemple.paymentservice.services.RentalPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rental-payments")
@RequiredArgsConstructor
public class RentalPaymentController {

    private final RentalPaymentService rentalPaymentService;

    @PostMapping("/plan")
    public ResponseEntity<List<PaiementLocation>> createPlan(
            @RequestBody CreateRentalPaymentPlanRequest request
    ) {
        return ResponseEntity.ok(rentalPaymentService.createPaymentPlan(request));
    }

    @GetMapping("/proposition/{propositionId}")
    public ResponseEntity<List<PaiementLocation>> getByProposition(
            @PathVariable Long propositionId
    ) {
        return ResponseEntity.ok(rentalPaymentService.getPaymentsByProposition(propositionId));
    }

    @GetMapping("/locataire/{locataireId}")
    public ResponseEntity<List<PaiementLocation>> getByLocataire(
            @PathVariable Long locataireId
    ) {
        return ResponseEntity.ok(rentalPaymentService.getPaymentsByLocataire(locataireId));
    }

    @PostMapping("/setup-card/{propositionId}")
    public ResponseEntity<Map<String, String>> setupCard(
            @PathVariable Long propositionId
    ) throws StripeException {
        return ResponseEntity.ok(rentalPaymentService.createSetupSession(propositionId));
    }

    @PostMapping("/charge-due-now")
    public ResponseEntity<String> chargeDueNow() {
        rentalPaymentService.chargeDuePayments();
        return ResponseEntity.ok("Due rental payments charged");
    }
}