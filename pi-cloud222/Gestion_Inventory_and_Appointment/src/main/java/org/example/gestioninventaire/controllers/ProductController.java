package org.example.gestioninventaire.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateProductRequest;
import org.example.gestioninventaire.dtos.request.UpdateProductRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.InventoryProductResponse;
import org.example.gestioninventaire.services.ProductCrudService;
import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    private final ProductCrudService productCrudService;
    private final JwtUtils jwtUtils;

    // ── CRUD de base ─────────────────────────────────────────────────

    @PostMapping
    public ApiResponse<InventoryProductResponse> create(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateProductRequest request) {
        request.setOwnerId(jwtUtils.extractUserId(authHeader));
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Produit créé avec succès")
                .data(productCrudService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<InventoryProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Produit mis à jour avec succès")
                .data(productCrudService.update(id, request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<InventoryProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Produit récupéré avec succès")
                .data(productCrudService.getById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<List<InventoryProductResponse>> getAll() {
        return ApiResponse.<List<InventoryProductResponse>>builder()
                .message("Liste des produits récupérée avec succès")
                .data(productCrudService.getAll())
                .build();
    }

    @GetMapping("/owner/{ownerId}")
    public ApiResponse<List<InventoryProductResponse>> getByOwner(@PathVariable Long ownerId) {
        return ApiResponse.<List<InventoryProductResponse>>builder()
                .message("Produits du propriétaire récupérés avec succès")
                .data(productCrudService.getByOwner(ownerId))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productCrudService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Produit supprimé avec succès")
                .data(null)
                .build();
    }

    // ── Boutique en ligne ─────────────────────────────────────────────

    /**
     * Agriculteur : voir la boutique publique d'un vétérinaire
     * GET /api/products/shop/vet/{vetId}
     */
    @GetMapping("/shop/vet/{vetId}")
    public ApiResponse<List<InventoryProductResponse>> getPublicShop(@PathVariable Long vetId) {
        return ApiResponse.<List<InventoryProductResponse>>builder()
                .message("Boutique récupérée avec succès")
                .data(productCrudService.getPublicShop(vetId))
                .build();
    }

    /**
     * Vétérinaire : mettre à jour les infos boutique d'un produit
     * (prix, description, image, visibilité) via multipart
     * PUT /api/products/{id}/boutique
     */
    @PutMapping(value = "/{id}/boutique", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<InventoryProductResponse> updateBoutique(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestParam(value = "prixVente",   required = false) Double prixVente,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "enBoutique",  required = false) Boolean enBoutique,
            @RequestParam(value = "image",       required = false) MultipartFile image
    ) throws IOException {
        Long vetId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Informations boutique mises à jour")
                .data(productCrudService.updateBoutiqueInfo(id, vetId, prixVente, description, enBoutique, image))
                .build();
    }

    /**
     * Vétérinaire : activer / désactiver un produit dans la boutique
     * PATCH /api/products/{id}/boutique/toggle
     */
    @PatchMapping("/{id}/boutique/toggle")
    public ApiResponse<InventoryProductResponse> toggleBoutique(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Long vetId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Visibilité boutique mise à jour")
                .data(productCrudService.toggleBoutique(id, vetId))
                .build();
    }

    /** Servir les images uploadées */
    @GetMapping("/images/{fileName}")
    public ResponseEntity<Resource> serveImage(@PathVariable String fileName) throws IOException {
        Path filePath = Paths.get("uploads/products/" + fileName);
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) return ResponseEntity.notFound().build();
        String contentType = Files.probeContentType(filePath);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .body(resource);
    }
}
