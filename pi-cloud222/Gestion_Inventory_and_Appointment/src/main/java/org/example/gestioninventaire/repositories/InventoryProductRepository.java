package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.InventoryProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryProductRepository extends JpaRepository<InventoryProduct, Long> {
    List<InventoryProduct> findByOwnerId(Long ownerId);
    boolean existsByNomAndOwnerId(String nom, Long ownerId);
    /** Pour la boutique publique agriculteur */
    List<InventoryProduct> findByOwnerIdAndEnBoutiqueTrue(Long ownerId);
}
