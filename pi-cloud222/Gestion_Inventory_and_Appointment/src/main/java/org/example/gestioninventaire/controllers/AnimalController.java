package org.example.gestioninventaire.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.AnimalDetailResponse;
import org.example.gestioninventaire.dtos.request.CreateAnimalRequest;
import org.example.gestioninventaire.dtos.request.UpdateAnimalRequest;
import org.example.gestioninventaire.dtos.response.AnimalSummaryResponse;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.services.AnimalCrudService;

import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name="Gestion Animal")
@RestController
@RequestMapping("/api/animals")
@RequiredArgsConstructor

public class AnimalController {

    private final AnimalCrudService animalCrudService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ApiResponse<AnimalSummaryResponse> create(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateAnimalRequest request
    ) {
        Long ownerId = jwtUtils.extractUserId(authHeader);
        request.setOwnerId(ownerId);
        return ApiResponse.<AnimalSummaryResponse>builder()
                .message("Animal créé avec succès")
                .data(animalCrudService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<AnimalSummaryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAnimalRequest request
    ) {
        return ApiResponse.<AnimalSummaryResponse>builder()
                .message("Animal mis à jour avec succès")
                .data(animalCrudService.update(id, request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<AnimalSummaryResponse> getById(@PathVariable Long id) {
        return ApiResponse.<AnimalSummaryResponse>builder()
                .message("Animal récupéré avec succès")
                .data(animalCrudService.getById(id))
                .build();
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<AnimalDetailResponse> getDetailById(@PathVariable Long id) {
        return ApiResponse.<AnimalDetailResponse>builder()
                .message("Détail animal récupéré avec succès")
                .data(animalCrudService.getDetailById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<List<AnimalSummaryResponse>> getAll() {
        return ApiResponse.<List<AnimalSummaryResponse>>builder()
                .message("Liste des animaux récupérée avec succès")
                .data(animalCrudService.getAll())
                .build();
    }

    @GetMapping("/my")
    public ApiResponse<List<AnimalSummaryResponse>> getMy(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long ownerId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<List<AnimalSummaryResponse>>builder()
                .message("Mes animaux récupérés avec succès")
                .data(animalCrudService.getByOwner(ownerId))
                .build();
    }

    @GetMapping("/owner/{ownerId}")
    public ApiResponse<List<AnimalSummaryResponse>> getByOwner(@PathVariable Long ownerId) {
        return ApiResponse.<List<AnimalSummaryResponse>>builder()
                .message("Animaux du propriétaire récupérés avec succès")
                .data(animalCrudService.getByOwner(ownerId))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        animalCrudService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Animal supprimé avec succès")
                .data(null)
                .build();
    }
}
