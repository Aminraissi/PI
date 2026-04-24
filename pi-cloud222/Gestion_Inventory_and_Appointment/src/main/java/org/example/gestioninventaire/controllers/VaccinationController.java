package org.example.gestioninventaire.controllers;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.CampaignAnimalDTO;
import org.example.gestioninventaire.dtos.VaccinationCampaignDTO;
import org.example.gestioninventaire.dtos.request.CreateVaccinationRequest;
import org.example.gestioninventaire.dtos.request.UpdateVaccinationRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.VaccinationResponse;
import org.example.gestioninventaire.entities.VaccinationCampaign;
import org.example.gestioninventaire.mappers.VaccinationMapper;
import org.example.gestioninventaire.services.VaccinationCrudService;
import org.example.gestioninventaire.services.VaccinationService;
import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vaccinations")
@RequiredArgsConstructor
public class VaccinationController {

    private final VaccinationCrudService vaccinationCrudService;
    private final VaccinationService service;
    private final VaccinationMapper mapper;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ApiResponse<VaccinationResponse> create(@Valid @RequestBody CreateVaccinationRequest request) {
        return ApiResponse.<VaccinationResponse>builder()
                .message("Vaccination créée avec succès")
                .data(vaccinationCrudService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<VaccinationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVaccinationRequest request
    ) {
        return ApiResponse.<VaccinationResponse>builder()
                .message("Vaccination mise à jour avec succès")
                .data(vaccinationCrudService.update(id, request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<VaccinationResponse> getById(@PathVariable Long id) {
        return ApiResponse.<VaccinationResponse>builder()
                .message("Vaccination récupérée avec succès")
                .data(vaccinationCrudService.getById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<List<VaccinationResponse>> getAll() {
        return ApiResponse.<List<VaccinationResponse>>builder()
                .message("Liste des vaccinations récupérée avec succès")
                .data(vaccinationCrudService.getAll())
                .build();
    }

    @GetMapping("/animal/{animalId}")
    public ApiResponse<List<VaccinationResponse>> getByAnimal(@PathVariable Long animalId) {
        return ApiResponse.<List<VaccinationResponse>>builder()
                .message("Vaccinations de l'animal récupérées avec succès")
                .data(vaccinationCrudService.getByAnimal(animalId))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        vaccinationCrudService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Vaccination supprimée avec succès")
                .data(null)
                .build();
    }

    @PostMapping("/campaign")
    public VaccinationCampaignDTO createCampaign(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody VaccinationCampaignDTO dto
    ) {
        Long ownerId = jwtUtils.extractUserId(authHeader);
        dto.setOwnerId(ownerId);

        VaccinationCampaign entity = mapper.toEntity(dto);
        // productId et dose sont dans l'entity, vaccin (String) n'est plus nécessaire
        VaccinationCampaign saved = service.createCampaign(entity, null, dto.getDose());

        return mapper.toDTO(saved);
    }

    // 🔹 vacciner tous
    @PostMapping("/campaign/{id}/vaccinate-all")
    public void vaccinateAll(@PathVariable Long id) {
        service.vaccinateAll(id);
    }

    // 🔹 vacciner un animal
    @PostMapping("/vaccination/{id}/done")
    public void vaccinateOne(@PathVariable Long id) {
        service.vaccinateOne(id);
    }

    // 🔹 progression
    @GetMapping("/campaign/{id}/progress")
    public double progress(@PathVariable Long id) {
        return service.getProgress(id);
    }
    @GetMapping("/campaigns")
    public List<VaccinationCampaignDTO> getAllCampaigns() {
        return service.getAllCampaigns();
    }
    // 🔹 2. détails campagne
    @GetMapping("/campaign/{id}")
    public VaccinationCampaignDTO getCampaign(@PathVariable Long id) {
        return service.getCampaignById(id);
    }

    // 🔹 3. animaux d’une campagne
    @GetMapping("/campaign/{id}/animals")
    public List<CampaignAnimalDTO> getAnimals(@PathVariable Long id) {
        return service.getAnimalsByCampaign(id);
    }
}
