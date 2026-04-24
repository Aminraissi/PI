package org.exemple.assistance_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.exemple.assistance_service.dto.DemandeAssistanceDTO;
import org.exemple.assistance_service.dto.ReponseIADTO;
import org.exemple.assistance_service.service.AssistanceWorkflowService;
import org.exemple.assistance_service.service.DemandeAssistanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class DemandeAssistanceController {

    private final DemandeAssistanceService service;
    private final AssistanceWorkflowService workflowService;

    @Operation(description = "add new demande")
    @PostMapping
    public ResponseEntity<DemandeAssistanceDTO> create(@RequestBody DemandeAssistanceDTO dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }
    @Operation(description = "update demande")
    @PutMapping("/{id}")
    public ResponseEntity<DemandeAssistanceDTO> update(@PathVariable Long id, @RequestBody DemandeAssistanceDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
    @Operation(description = "get demande by id")
    @GetMapping("/{id}")
    public ResponseEntity<DemandeAssistanceDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }
    @Operation(description = "generate AI response for demande")
    @PostMapping("/{id}/generate-ai")
    public ResponseEntity<ReponseIADTO> generateAI(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.generateAIResponse(id));
    }
    @Operation(description = "get all demande")
    @GetMapping
    public ResponseEntity<List<DemandeAssistanceDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
    @Operation(description = "get demande by user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DemandeAssistanceDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }
    @Operation(description = "delete demande")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
