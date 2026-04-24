package org.example.gestionvente.Repositories;

import org.example.gestionvente.Entities.CommandeLigne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandeLigneRepo extends JpaRepository<CommandeLigne, Long> {

    List<CommandeLigne> findByCommandeId(Long commandeId);

    boolean existsByProduitId(Long produitId);

    boolean existsByCommandeIdAndProduitId(Long commandeId, Long produitId);
}