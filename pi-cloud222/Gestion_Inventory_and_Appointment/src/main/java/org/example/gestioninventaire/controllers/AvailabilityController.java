package org.example.gestioninventaire.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.BlockDayRequest;
import org.example.gestioninventaire.dtos.request.CreateAvailabilityRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.VeterinarianAvailabilityResponse;
import org.example.gestioninventaire.services.AvailabilityService;
import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availabilities")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ApiResponse<VeterinarianAvailabilityResponse> createAvailability(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateAvailabilityRequest request
    ) {
        // veterinarianId extrait du JWT — pas besoin de l'envoyer dans le body
        Long veterinarianId = jwtUtils.extractUserId(authHeader);
        request.setVeterinarianId(veterinarianId);
        return ApiResponse.<VeterinarianAvailabilityResponse>builder()
                .message("Disponibilité créée avec succès")
                .data(availabilityService.createAvailability(request))
                .build();
    }

    @GetMapping("/veterinarian/{veterinarianId}")
    public ApiResponse<List<VeterinarianAvailabilityResponse>> getVetAvailabilities(
            @PathVariable Long veterinarianId
    ) {
        return ApiResponse.<List<VeterinarianAvailabilityResponse>>builder()
                .message("Disponibilités du vétérinaire récupérées avec succès")
                .data(availabilityService.getVetAvailabilities(veterinarianId))
                .build();
    }

    @GetMapping("/my")
    public ApiResponse<List<VeterinarianAvailabilityResponse>> getMyAvailabilities(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long veterinarianId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<List<VeterinarianAvailabilityResponse>>builder()
                .message("Mes disponibilités récupérées avec succès")
                .data(availabilityService.getVetAvailabilities(veterinarianId))
                .build();
    }

    @PutMapping("/block-day")
    public ApiResponse<Void> blockDay(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody BlockDayRequest request
    ) {
        Long veterinarianId = jwtUtils.extractUserId(authHeader);
        availabilityService.blockDay(veterinarianId, request.getDate());
        return ApiResponse.<Void>builder()
                .message("Jour bloqué avec succès")
                .data(null)
                .build();
    }
}
