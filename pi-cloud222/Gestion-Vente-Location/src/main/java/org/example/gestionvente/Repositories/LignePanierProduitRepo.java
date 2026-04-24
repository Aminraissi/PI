package org.example.gestionvente.Repositories;

import org.example.gestionvente.Entities.LignePanierProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LignePanierProduitRepo extends JpaRepository<LignePanierProduit, Long> {

    List<LignePanierProduit> findByPanierId(Long panierId);

    Optional<LignePanierProduit> findByPanierIdAndProduitId(Long panierId, Long produitId);
}