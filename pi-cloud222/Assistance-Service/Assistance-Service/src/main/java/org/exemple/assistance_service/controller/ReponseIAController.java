package org.exemple.assistance_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.exemple.assistance_service.dto.ReponseIADTO;
import org.exemple.assistance_service.service.ReponseIAService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequiredArgsConstructor
public class ReponseIAController {

    private final ReponseIAService service;
    @Operation(description = "add new reponse-ia")
    @PostMapping("/api/demandes/{demandeId}/reponse-ia")
    public ResponseEntity<ReponseIADTO> create(@PathVariable Long demandeId, @RequestBody ReponseIADTO dto) {
        return new ResponseEntity<>(service.create(demandeId, dto), HttpStatus.CREATED);
    }
    @Operation(description = "update reponse-ia")
    @PutMapping("/api/reponses-ia/{id}")
    public ResponseEntity<ReponseIADTO> update(@PathVariable Long id, @RequestBody ReponseIADTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
    @Operation(description = "get reponse-ia by id")
    @GetMapping("/api/reponses-ia/{id}")
    public ResponseEntity<ReponseIADTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }
    @Operation(description = "get all reponse-ia")
    @GetMapping("/api/reponses-ia")
    public ResponseEntity<List<ReponseIADTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
    @Operation(description = "delete reponse-ia")
    @DeleteMapping("/api/reponses-ia/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}