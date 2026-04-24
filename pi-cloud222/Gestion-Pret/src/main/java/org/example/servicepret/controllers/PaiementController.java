package org.example.servicepret.controllers;

import lombok.AllArgsConstructor;

import org.example.servicepret.entities.Paiement;
import org.example.servicepret.services.IPaiementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/paiement")
public class PaiementController {
    private IPaiementService paiementService;

    @GetMapping("/getAll")
    public List<Paiement> recupererTousLesPaiements()
    {
        return paiementService.retrieveAllPaiements();
    }
    @PostMapping("/add")
    public Paiement ajouterPaiement(@RequestBody Paiement p)
    {
        return paiementService.addPaiement(p);
    }

    @PutMapping("/update")
    public Paiement updatePaiement(@RequestBody Paiement p)
    {
        return paiementService.updatePaiement(p);
    }
    @GetMapping("/get/{id}")
    public Paiement retrievePaiement(@PathVariable long id)
    {
        return paiementService.retrievePaiement(id);
    }

    @DeleteMapping("/delete/{id}")
    public void supprimerPaiement(@PathVariable long id) {

        paiementService.removePaiement(id);
    }

    @PostMapping("/create-bank-intent")
    public ResponseEntity<Map<String, String>> createBankIntent(@RequestParam long montant) {
        try {
            String clientSecret = paiementService.creerPaymentIntentVirement(montant);
            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", clientSecret);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/create-intent")
    public ResponseEntity<Map<String, String>> createIntent(@RequestParam long montant) {
        try {
            String clientSecret = paiementService.creerPaymentIntent(montant);
            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", clientSecret);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/create-agriculteur-intent/{pretId}")
    public ResponseEntity<Map<String, String>> createAgriculteurIntent(@PathVariable Long pretId) {
        try {
            String clientSecret = paiementService.creerPaymentIntentAgriculteur(pretId);

            Map<String, String> res = new HashMap<>();
            res.put("clientSecret", clientSecret);

            return ResponseEntity.ok(res);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}

