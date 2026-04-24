package org.example.gestioninventaire.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CommentaireAvisRequest;
import org.example.gestioninventaire.dtos.request.CreateAvisRequest;
import org.example.gestioninventaire.dtos.request.ReponseAvisRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.AvisResponse;
import org.example.gestioninventaire.dtos.response.CommentaireAvisResponse;
import org.example.gestioninventaire.dtos.response.ReponseAvisResponse;
import org.example.gestioninventaire.dtos.response.VetRatingSummaryResponse;
import org.example.gestioninventaire.services.AvisService;
import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/avis")
@RequiredArgsConstructor
public class AvisController {

    private final AvisService avisService;
    private final JwtUtils jwtUtils;

    // ─────────────────────────────────────────────────────────────
    // POST /api/avis
    // Agriculteur publie un avis (note + commentaire) sur un vétérinaire
    // ─────────────────────────────────────────────────────────────
    @PostMapping
    public ApiResponse<AvisResponse> createAvis(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateAvisRequest req) {

        Long agriculteurId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<AvisResponse>builder()
                .message("Avis publié avec succès")
                .data(avisService.createAvis(req, agriculteurId))
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/avis/vet/{vetId}
    // Récupère tous les avis d'un vétérinaire
    // Le champ likedByMe est calculé pour l'utilisateur JWT courant
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/vet/{vetId}")
    public ApiResponse<List<AvisResponse>> getAvisByVet(
            @PathVariable Long vetId,
            @RequestHeader("Authorization") String authHeader) {

        Long currentUserId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<List<AvisResponse>>builder()
                .message("Avis récupérés avec succès")
                .data(avisService.getAvisByVet(vetId, currentUserId))
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/avis/vet/{vetId}/summary
    // Résumé : moyenne des étoiles + total avis + distribution par note
    // Accessible sans JWT (public)
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/vet/{vetId}/summary")
    public ApiResponse<VetRatingSummaryResponse> getRatingSummary(
            @PathVariable Long vetId) {

        return ApiResponse.<VetRatingSummaryResponse>builder()
                .message("Résumé des évaluations récupéré")
                .data(avisService.getRatingSummary(vetId))
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/avis/{avisId}/like
    // Toggle like : si déjà liké → unlike, sinon → like
    // Réservé aux agriculteurs (pas son propre avis)
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/{avisId}/like")
    public ApiResponse<Void> toggleLike(
            @PathVariable Long avisId,
            @RequestHeader("Authorization") String authHeader) {

        Long agriculteurId = jwtUtils.extractUserId(authHeader);
        avisService.toggleLike(avisId, agriculteurId);
        return ApiResponse.<Void>builder()
                .message("Like mis à jour")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/avis/{avisId}/commentaires
    // Un agriculteur répond au commentaire d'un autre agriculteur
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/{avisId}/commentaires")
    public ApiResponse<CommentaireAvisResponse> addCommentaire(
            @PathVariable Long avisId,
            @Valid @RequestBody CommentaireAvisRequest req,
            @RequestHeader("Authorization") String authHeader) {

        Long agriculteurId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<CommentaireAvisResponse>builder()
                .message("Commentaire ajouté avec succès")
                .data(avisService.addCommentaire(avisId, req, agriculteurId))
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/avis/{avisId}/reponse
    // Le vétérinaire répond officiellement à un avis (une fois seulement)
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/{avisId}/reponse")
    public ApiResponse<ReponseAvisResponse> addReponseVet(
            @PathVariable Long avisId,
            @Valid @RequestBody ReponseAvisRequest req,
            @RequestHeader("Authorization") String authHeader) {

        Long vetId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<ReponseAvisResponse>builder()
                .message("Réponse publiée avec succès")
                .data(avisService.addReponseVet(avisId, req, vetId))
                .build();
    }
}
