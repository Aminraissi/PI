package org.exemple.assistance_service.controller;

import lombok.RequiredArgsConstructor;
import org.exemple.assistance_service.dto.AffectationDemandeDTO;
import org.exemple.assistance_service.service.AffectationDemandeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequiredArgsConstructor
public class AffectationDemandeController {

    private final AffectationDemandeService service;

    @PostMapping("/api/demandes/{demandeId}/affectation")
    public ResponseEntity<AffectationDemandeDTO> create(@PathVariable Long demandeId, @RequestBody AffectationDemandeDTO dto) {
        return new ResponseEntity<>(service.create(demandeId, dto), HttpStatus.CREATED);
    }

    @PutMapping("/api/affectations/{id}")
    public ResponseEntity<AffectationDemandeDTO> update(@PathVariable Long id, @RequestBody AffectationDemandeDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @GetMapping("/api/affectations/{id}")
    public ResponseEntity<AffectationDemandeDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/api/affectations")
    public ResponseEntity<List<AffectationDemandeDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/api/affectations/ingenieur/{ingenieurId}")
    public ResponseEntity<List<AffectationDemandeDTO>> getByIngenieurId(@PathVariable Long ingenieurId) {
        return ResponseEntity.ok(service.getByIngenieurId(ingenieurId));
    }

    @GetMapping("/api/affectations/ingenieur/{ingenieurId}/pending")
    public ResponseEntity<List<AffectationDemandeDTO>> getPendingByIngenieurId(@PathVariable Long ingenieurId) {
        return ResponseEntity.ok(service.getPendingByIngenieurId(ingenieurId));
    }

    @PostMapping("/api/affectations/{id}/accept")
    public ResponseEntity<AffectationDemandeDTO> accept(@PathVariable Long id, @RequestParam Long ingenieurId) {
        return ResponseEntity.ok(service.accept(id, ingenieurId));
    }

    @PostMapping("/api/affectations/{id}/refuse")
    public ResponseEntity<AffectationDemandeDTO> refuse(@PathVariable Long id, @RequestParam Long ingenieurId) {
        return ResponseEntity.ok(service.refuse(id, ingenieurId));
    }

    @DeleteMapping("/api/affectations/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
