package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    // JOIN FETCH product pour éviter LazyInitializationException
    @Query("SELECT m FROM StockMovement m JOIN FETCH m.product ORDER BY m.dateMouvement DESC")
    List<StockMovement> findAllByOrderByDateMouvementDesc();

    @Query("SELECT m FROM StockMovement m JOIN FETCH m.product WHERE m.product.id = :productId ORDER BY m.dateMouvement DESC")
    List<StockMovement> findByProductIdOrderByDateMouvementDesc(@Param("productId") Long productId);

    @Query("SELECT m FROM StockMovement m JOIN FETCH m.product WHERE m.ownerId = :ownerId ORDER BY m.dateMouvement DESC")
    List<StockMovement> findByOwnerIdOrderByDateMouvementDesc(@Param("ownerId") Long ownerId);
    void deleteByProductId(Long productId);
}
