package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.LikeAvis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeAvisRepository extends JpaRepository<LikeAvis, Long> {

    /** Trouve un like par avis et agriculteur */
    Optional<LikeAvis> findByAvisIdAndAgriculteurId(Long avisId, Long agriculteurId);

    /** Vérifie si un agriculteur a déjà liké un avis */
    boolean existsByAvisIdAndAgriculteurId(Long avisId, Long agriculteurId);

    /** Nombre de likes pour un avis */
    long countByAvisId(Long avisId);
}
