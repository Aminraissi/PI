package org.exemple.assistance_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.exemple.assistance_service.dto.ReponseIngenieurDTO;
import org.exemple.assistance_service.service.ReponseIngenieurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequiredArgsConstructor
public class ReponseIngenieurController {

    private final ReponseIngenieurService service;
    @Operation(description = "add new affectations Reponse Ingenieur ")
    @PostMapping("/api/affectations/{affectationId}/reponses")
    public ResponseEntity<ReponseIngenieurDTO> create(@PathVariable Long affectationId, @RequestBody ReponseIngenieurDTO dto) {
        return new ResponseEntity<>(service.create(affectationId, dto), HttpStatus.CREATED);
    }
    @Operation(description = "update affectations Reponse Ingenieur ")
    @PutMapping("/api/reponses-ingenieur/{id}")
    public ResponseEntity<ReponseIngenieurDTO> update(@PathVariable Long id, @RequestBody ReponseIngenieurDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
    @Operation(description = "get by id affectations Reponse Ingenieur ")
    @GetMapping("/api/reponses-ingenieur/{id}")
    public ResponseEntity<ReponseIngenieurDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }
    @Operation(description = "get all affectations Reponse Ingenieur ")
    @GetMapping("/api/reponses-ingenieur")
    public ResponseEntity<List<ReponseIngenieurDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
    @Operation(description = "delete affectations Reponse Ingenieur ")
    @DeleteMapping("/api/reponses-ingenieur/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}