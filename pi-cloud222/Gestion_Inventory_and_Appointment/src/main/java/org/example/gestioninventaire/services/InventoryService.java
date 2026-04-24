package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.AddStockRequest;
import org.example.gestioninventaire.dtos.request.AdjustStockRequest;
import org.example.gestioninventaire.dtos.request.ConsumeStockRequest;
import org.example.gestioninventaire.dtos.response.BatchResponse;
import org.example.gestioninventaire.dtos.response.InventoryProductResponse;
import org.example.gestioninventaire.dtos.response.StockMovementResponse;
import org.example.gestioninventaire.entities.Batch;
import org.example.gestioninventaire.entities.InventoryProduct;
import org.example.gestioninventaire.entities.StockMovement;
import org.example.gestioninventaire.enums.MovementType;
import org.example.gestioninventaire.enums.Reason;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.mappers.InventoryMapper;
import org.example.gestioninventaire.repositories.BatchRepository;
import org.example.gestioninventaire.repositories.InventoryProductRepository;
import org.example.gestioninventaire.repositories.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional
    public InventoryProductResponse addStock(Long productId, AddStockRequest request, Long userId) {
        InventoryProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé"));

        product.setCurrentQuantity((product.getCurrentQuantity() == null ? 0.0 : product.getCurrentQuantity()) + request.getQuantity());
        productRepository.save(product);

        Batch batch = Batch.builder()
                .lotNumber("LOT-" + UUID.randomUUID().toString().substring(0, 8))
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .purchaseDate(request.getPurchaseDate())
                .expiryDate(request.getExpiryDate())
                .note(request.getNote())
                .product(product)
                .build();
        batchRepository.save(batch);
        refreshProductStock(product);

        StockMovement movement = StockMovement.builder()
                .movementType(MovementType.IN)
                .quantity(request.getQuantity())
                .dateMouvement(LocalDateTime.now())
                .reason(Reason.ACHAT)
                .note(request.getNote())
                .product(product)
                .ownerId(userId)
                .build();
        stockMovementRepository.save(movement);


        return inventoryMapper.toProductResponse(product);
    }

    @Transactional
    public InventoryProductResponse consumeStock(Long productId, ConsumeStockRequest request, Long userId) {
        InventoryProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé"));

        if (request.getQuantity() <= 0) throw new BadRequestException("La quantité doit être positive");
        if (product.getCurrentQuantity() < request.getQuantity()) throw new BadRequestException("Stock insuffisant");

        product.setCurrentQuantity(product.getCurrentQuantity() - request.getQuantity());
        productRepository.save(product);

        List<Batch> batches = batchRepository.findByProductIdOrderByPurchaseDateAsc(productId);
        double remaining = request.getQuantity();
        for (Batch batch : batches) {
            if (remaining <= 0) break;
            double batchQty = batch.getQuantity();
            if (batchQty <= remaining) { remaining -= batchQty; batch.setQuantity(0.0); }
            else { batch.setQuantity(batchQty - remaining); remaining = 0; }
            batchRepository.save(batch);

        }
        refreshProductStock(product);

        StockMovement movement = StockMovement.builder()
                .movementType(MovementType.OUT)
                .quantity(request.getQuantity())
                .dateMouvement(LocalDateTime.now())
                .reason(request.getReason())
                .note(request.getNote())
                .product(product)
                .ownerId(userId)
                .build();
        stockMovementRepository.save(movement);

        return inventoryMapper.toProductResponse(product);
    }

    @Transactional
    public InventoryProductResponse adjustStock(Long productId, AdjustStockRequest request, Long userId) {
        InventoryProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé"));

        double newQty = product.getCurrentQuantity() + request.getQuantity();
        if (newQty < 0) throw new BadRequestException("La quantité finale ne peut pas être négative");

        product.setCurrentQuantity(newQty);
        productRepository.save(product);
        refreshProductStock(product);

        StockMovement movement = StockMovement.builder()
                .movementType(MovementType.ADJUST)
                .quantity(request.getQuantity())
                .dateMouvement(LocalDateTime.now())
                .reason(Reason.AJUSTEMENT)
                .note(request.getNote())
                .product(product)
                .ownerId(userId)
                .build();
        stockMovementRepository.save(movement);

        return inventoryMapper.toProductResponse(product);
    }

    public List<StockMovementResponse> getAllMovements() {
        return stockMovementRepository.findAllByOrderByDateMouvementDesc()
                .stream().map(inventoryMapper::toMovementResponse).toList();
    }

    public List<StockMovementResponse> getMyMovements(Long ownerId) {
        return stockMovementRepository.findByOwnerIdOrderByDateMouvementDesc(ownerId)
                .stream().map(inventoryMapper::toMovementResponse).toList();
    }

    public List<StockMovementResponse> getProductMovements(Long productId) {
        return stockMovementRepository.findByProductIdOrderByDateMouvementDesc(productId)
                .stream().map(inventoryMapper::toMovementResponse).toList();
    }

    public List<BatchResponse> getProductBatches(Long productId) {
        if (!productRepository.existsById(productId)) throw new ResourceNotFoundException("Produit non trouvé");
        return batchRepository.findByProductIdOrderByPurchaseDateAsc(productId)
                .stream().map(inventoryMapper::toBatchResponse).toList();
    }
    private void refreshProductStock(InventoryProduct product) {
        Double total = batchRepository.sumQuantityByProductId(product.getId());
        product.setCurrentQuantity(total != null ? total : 0.0);
        productRepository.save(product);
    }
}
