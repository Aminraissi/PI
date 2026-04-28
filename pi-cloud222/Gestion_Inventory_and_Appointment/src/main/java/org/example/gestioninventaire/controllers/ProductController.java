package org.example.gestioninventaire.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateProductRequest;
import org.example.gestioninventaire.dtos.request.UpdateProductRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.InventoryProductResponse;
import org.example.gestioninventaire.services.ProductCrudService;
import org.example.gestioninventaire.services.ShopSearchService;
import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductCrudService productCrudService;
    private final JwtUtils jwtUtils;
    private final ShopSearchService shopSearchService;

    @PostMapping
    public ApiResponse<InventoryProductResponse> create(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateProductRequest request
    ) {
        Long ownerId = jwtUtils.extractUserId(authHeader);
        request.setOwnerId(ownerId);
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Produit cree avec succes")
                .data(productCrudService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<InventoryProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Produit mis a jour avec succes")
                .data(productCrudService.update(id, request))
                .build();
    }

    @PutMapping(value = "/{id}/boutique", consumes = "multipart/form-data")
    public ApiResponse<InventoryProductResponse> updateBoutiqueInfo(
            @PathVariable Long id,
            @RequestParam(value = "prixVente", required = false) Double prixVente,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "enBoutique", required = false) Boolean enBoutique,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Infos boutique mises a jour")
                .data(productCrudService.updateBoutiqueInfo(id, prixVente, description, enBoutique, image))
                .build();
    }

    @PatchMapping("/{id}/boutique/toggle")
    public ApiResponse<InventoryProductResponse> toggleBoutique(@PathVariable Long id) {
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Visibilite boutique mise a jour")
                .data(productCrudService.toggleBoutique(id))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<InventoryProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Produit recupere avec succes")
                .data(productCrudService.getById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<List<InventoryProductResponse>> getAll() {
        return ApiResponse.<List<InventoryProductResponse>>builder()
                .message("Liste des produits recuperee avec succes")
                .data(productCrudService.getAll())
                .build();
    }

    @GetMapping("/owner/{ownerId}")
    public ApiResponse<List<InventoryProductResponse>> getByOwner(@PathVariable Long ownerId) {
        return ApiResponse.<List<InventoryProductResponse>>builder()
                .message("Produits du proprietaire recuperes avec succes")
                .data(productCrudService.getByOwner(ownerId))
                .build();
    }

    @GetMapping("/shop/vet/{ownerId}")
    public ApiResponse<List<InventoryProductResponse>> getPublicShopByVet(@PathVariable Long ownerId) {
        return ApiResponse.<List<InventoryProductResponse>>builder()
                .message("Boutique du veterinaire recuperee avec succes")
                .data(productCrudService.getPublicShopByVet(ownerId))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productCrudService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Produit supprime avec succes")
                .data(null)
                .build();
    }

    @GetMapping("/products/{id}")
    public ApiResponse<InventoryProductResponse> getProductById(@PathVariable Long id) {
        return ApiResponse.<InventoryProductResponse>builder()
                .data(productCrudService.getById(id))
                .message("Produit recupere")
                .build();
    }

    @GetMapping("/shop/all")
    public ApiResponse<List<InventoryProductResponse>> getAllPublicShop() {
        return ApiResponse.<List<InventoryProductResponse>>builder()
                .message("Boutique globale recuperee avec succes")
                .data(productCrudService.getAllPublicShop())
                .build();
    }

    @PostMapping("/shop/search-ai")
    public ApiResponse<List<InventoryProductResponse>> searchWithAI(@RequestBody Map<String, String> body) {
        String query = body.getOrDefault("query", "");
        return ApiResponse.<List<InventoryProductResponse>>builder()
                .message("Resultats de la recherche IA")
                .data(shopSearchService.searchWithAI(query))
                .build();
    }
}
