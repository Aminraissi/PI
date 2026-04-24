package org.example.gestioninventaire.controllers;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateHealthRecordRequest;
import org.example.gestioninventaire.dtos.request.MedicalAssistantQuestionRequest;
import org.example.gestioninventaire.dtos.request.UpdateHealthRecordRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.HealthRecordResponse;
import org.example.gestioninventaire.dtos.response.MedicalAssistantAnswerResponse;
import org.example.gestioninventaire.services.HealthRecordCrudService;
import org.example.gestioninventaire.services.VetMedicalAssistantService;
import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-records")
@RequiredArgsConstructor
public class HealthRecordController {

    private final HealthRecordCrudService healthRecordCrudService;
    private final VetMedicalAssistantService vetMedicalAssistantService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ApiResponse<HealthRecordResponse> create(@Valid @RequestBody CreateHealthRecordRequest request) {
        return ApiResponse.<HealthRecordResponse>builder()
                .message("Dossier santé créé avec succès")
                .data(healthRecordCrudService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<HealthRecordResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHealthRecordRequest request
    ) {
        return ApiResponse.<HealthRecordResponse>builder()
                .message("Dossier santé mis à jour avec succès")
                .data(healthRecordCrudService.update(id, request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<HealthRecordResponse> getById(@PathVariable Long id) {
        return ApiResponse.<HealthRecordResponse>builder()
                .message("Dossier santé récupéré avec succès")
                .data(healthRecordCrudService.getById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<List<HealthRecordResponse>> getAll() {
        return ApiResponse.<List<HealthRecordResponse>>builder()
                .message("Liste des dossiers santé récupérée avec succès")
                .data(healthRecordCrudService.getAll())
                .build();
    }

    @GetMapping("/animal/{animalId}")
    public ApiResponse<List<HealthRecordResponse>> getByAnimal(@PathVariable Long animalId) {
        return ApiResponse.<List<HealthRecordResponse>>builder()
                .message("Historique santé de l'animal récupéré avec succès")
                .data(healthRecordCrudService.getByAnimal(animalId))
                .build();
    }

    @PostMapping("/animal/{animalId}/assistant")
    public ApiResponse<MedicalAssistantAnswerResponse> askMedicalAssistant(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long animalId,
            @Valid @RequestBody MedicalAssistantQuestionRequest request
    ) {
        Long veterinarianId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<MedicalAssistantAnswerResponse>builder()
                .message("Reponse du chatbot medical generee avec succes")
                .data(vetMedicalAssistantService.askQuestion(veterinarianId, animalId, request.getQuestion()))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        healthRecordCrudService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Dossier santé supprimé avec succès")
                .data(null)
                .build();
    }
}