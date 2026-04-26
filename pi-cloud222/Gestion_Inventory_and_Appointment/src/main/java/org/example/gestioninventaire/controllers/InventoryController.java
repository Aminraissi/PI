package org.example.gestioninventaire.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.AddStockRequest;
import org.example.gestioninventaire.dtos.request.AdjustStockRequest;
import org.example.gestioninventaire.dtos.request.ConsumeBatchRequest;
import org.example.gestioninventaire.dtos.request.ConsumeStockRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.BatchResponse;
import org.example.gestioninventaire.dtos.response.InventoryProductResponse;
import org.example.gestioninventaire.dtos.response.StockMovementResponse;
import org.example.gestioninventaire.services.InventoryService;

import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final JwtUtils jwtUtils;

    @PostMapping("/{productId}/add-stock")
    public ApiResponse<InventoryProductResponse> addStock(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId,
            @Valid @RequestBody AddStockRequest request
    ) {
        Long userId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Stock ajouté avec succès")
                .data(inventoryService.addStock(productId, request, userId))
                .build();
    }

    @PostMapping("/{productId}/consume")
    public ApiResponse<InventoryProductResponse> consume(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId,
            @Valid @RequestBody ConsumeStockRequest request
    ) {
        Long userId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Consommation enregistrée avec succès")
                .data(inventoryService.consumeStock(productId, request, userId))
                .build();
    }

    @PostMapping("/{productId}/batches/{batchId}/consume")
    public ApiResponse<InventoryProductResponse> consumeFromBatch(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId,
            @PathVariable Long batchId,
            @Valid @RequestBody ConsumeBatchRequest request
    ) {
        Long userId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Consommation du lot enregistree avec succes")
                .data(inventoryService.consumeStockFromBatch(productId, batchId, request, userId))
                .build();
    }

    @PostMapping("/{productId}/adjust")
    public ApiResponse<InventoryProductResponse> adjust(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId,
            @Valid @RequestBody AdjustStockRequest request
    ) {
        Long userId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<InventoryProductResponse>builder()
                .message("Ajustement effectué avec succès")
                .data(inventoryService.adjustStock(productId, request, userId))
                .build();
    }

    @GetMapping("/movements")
    public ApiResponse<List<StockMovementResponse>> getAllMovements() {
        return ApiResponse.<List<StockMovementResponse>>builder()
                .message("Liste des mouvements récupérée avec succès")
                .data(inventoryService.getAllMovements())
                .build();
    }

    @GetMapping("/my-movements")
    public ApiResponse<List<StockMovementResponse>> getMyMovements(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long ownerId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<List<StockMovementResponse>>builder()
                .message("Mes mouvements de stock récupérés avec succès")
                .data(inventoryService.getMyMovements(ownerId))
                .build();
    }

    @GetMapping("/{productId}/movements")
    public ApiResponse<List<StockMovementResponse>> getProductMovements(@PathVariable Long productId) {
        return ApiResponse.<List<StockMovementResponse>>builder()
                .message("Mouvements du produit récupérés avec succès")
                .data(inventoryService.getProductMovements(productId))
                .build();
    }

    @GetMapping("/{productId}/batches")
    public ApiResponse<List<BatchResponse>> getProductBatches(@PathVariable Long productId) {
        return ApiResponse.<List<BatchResponse>>builder()
                .message("Lots du produit récupérés avec succès")
                .data(inventoryService.getProductBatches(productId))
                .build();
    }
}
