package org.example.gestioninventaire.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateUnavailabilityRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.UnavailabilityResponse;
import org.example.gestioninventaire.services.UnavailabilityService;

import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unavailabilities")
@RequiredArgsConstructor
public class UnavailabilityController {

    private final UnavailabilityService unavailabilityService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ApiResponse<Void> create(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateUnavailabilityRequest request
    ) {
        Long veterinarianId = jwtUtils.extractUserId(authHeader);
        unavailabilityService.createUnavailability(veterinarianId, request);
        return ApiResponse.<Void>builder()
                .message("Indisponibilité enregistrée avec succès")
                .data(null)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id
    ) {
        Long veterinarianId = jwtUtils.extractUserId(authHeader);
        unavailabilityService.deleteUnavailability(veterinarianId, id);
        return ApiResponse.<Void>builder()
                .message("Indisponibilité supprimée avec succès")
                .data(null)
                .build();
    }

    @GetMapping
    public ApiResponse<List<UnavailabilityResponse>> getMyUnavailabilities(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long veterinarianId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<List<UnavailabilityResponse>>builder()
                .message("Indisponibilités récupérées avec succès")
                .data(unavailabilityService.getByVeterinarian(veterinarianId))
                .build();
    }
}
