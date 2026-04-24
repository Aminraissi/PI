package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    // JOIN FETCH product pour éviter LazyInitializationException
    @Query("SELECT b FROM Batch b JOIN FETCH b.product WHERE b.product.id = :productId ORDER BY b.purchaseDate ASC")
    List<Batch> findByProductIdOrderByPurchaseDateAsc(@Param("productId") Long productId);
    @Query("select coalesce(sum(b.quantity), 0) from Batch b where b.product.id = :productId")
    Double sumQuantityByProductId(@Param("productId") Long productId);
    void deleteByProductId(Long productId);
}
