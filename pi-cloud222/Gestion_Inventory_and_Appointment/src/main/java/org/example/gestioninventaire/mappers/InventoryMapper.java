package org.example.gestioninventaire.mappers;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.response.BatchResponse;
import org.example.gestioninventaire.dtos.response.InventoryProductResponse;
import org.example.gestioninventaire.dtos.response.StockMovementResponse;
import org.example.gestioninventaire.dtos.response.UserSummaryResponse;
import org.example.gestioninventaire.entities.Batch;
import org.example.gestioninventaire.entities.InventoryProduct;
import org.example.gestioninventaire.entities.StockMovement;
import org.example.gestioninventaire.feigns.UserClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryMapper {

    private final UserClient userClient;
    private final UserMapper userMapper;

    public InventoryProductResponse toProductResponse(InventoryProduct product) {
        if (product == null) return null;

        UserSummaryResponse owner = null;
        if (product.getOwnerId() != null) {
            try {
                owner = userMapper.toSummary(userClient.getUserById(product.getOwnerId()));
            } catch (Exception e) {
                owner = null;
            }
        }

        return InventoryProductResponse.builder()
                .id(product.getId())
                .nom(product.getNom())
                .categorie(product.getCategorie())
                .unit(product.getUnit())
                .isPerishable(product.getIsPerishable())
                .currentQuantity(product.getCurrentQuantity())
                .minThreshold(product.getMinThreshold())
                .owner(owner)
                // Boutique fields
                .prixVente(product.getPrixVente())
                .imageUrl(product.getImageUrl())
                .description(product.getDescription())
                .enBoutique(product.getEnBoutique() != null ? product.getEnBoutique() : false)
                .enStock(product.getCurrentQuantity() != null && product.getCurrentQuantity() > 0)
                .build();
    }

    public BatchResponse toBatchResponse(Batch batch) {
        if (batch == null) return null;

        return BatchResponse.builder()
                .id(batch.getId())
                .lotNumber(batch.getLotNumber())
                .quantity(batch.getQuantity())
                .price(batch.getPrice())
                .expiryDate(batch.getExpiryDate())
                .purchaseDate(batch.getPurchaseDate())
                .note(batch.getNote())
                .build();
    }

    public StockMovementResponse toMovementResponse(StockMovement movement) {
        if (movement == null) return null;

        UserSummaryResponse user = null;
        if (movement.getOwnerId() != null) {
            try {
                user = userMapper.toSummary(userClient.getUserById(movement.getOwnerId()));
            } catch (Exception e) {
                user = null;
            }
        }

        return StockMovementResponse.builder()
                .id(movement.getId())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .dateMouvement(movement.getDateMouvement())
                .reason(movement.getReason())
                .note(movement.getNote())
                .productId(movement.getProduct() != null ? movement.getProduct().getId() : null)
                .productName(movement.getProduct() != null ? movement.getProduct().getNom() : null)
                .user(user)
                .build();
    }
}
