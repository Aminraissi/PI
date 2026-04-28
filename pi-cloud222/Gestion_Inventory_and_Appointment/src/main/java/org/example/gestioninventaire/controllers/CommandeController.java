package org.example.gestioninventaire.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gestioninventaire.dtos.request.CommandeRequest;
import org.example.gestioninventaire.dtos.response.CommandeResponse;
import org.example.gestioninventaire.dtos.response.CommandeVetResponse;
import org.example.gestioninventaire.services.CommandeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class CommandeController {

    private final CommandeService commandeService;

    @PostMapping
    public ResponseEntity<CommandeResponse> creerCommande(@RequestBody CommandeRequest request) {
        CommandeResponse response = commandeService.creerCommande(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/agriculteur/{id}")
    public ResponseEntity<List<CommandeResponse>> getMesCommandes(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.getMesCommandes(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandeResponse> getCommande(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.getCommande(id));
    }

    @PostMapping("/{id}/confirmer-paiement")
    public ResponseEntity<Map<String, String>> confirmerPaiementCommande(@PathVariable Long id) {
        commandeService.confirmerPaiementCommande(id);
        return ResponseEntity.ok(Map.of("message", "Commande marquee PAYE"));
    }

    @GetMapping("/vet/{vetId}")
    public ResponseEntity<List<CommandeVetResponse>> getCommandesVet(@PathVariable Long vetId) {
        return ResponseEntity.ok(commandeService.getCommandesByVetId(vetId));
    }
}
