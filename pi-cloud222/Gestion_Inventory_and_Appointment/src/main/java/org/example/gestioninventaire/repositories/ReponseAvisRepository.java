package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.ReponseAvis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReponseAvisRepository extends JpaRepository<ReponseAvis, Long> {

    /** Trouve la réponse vétérinaire associée à un avis */
    Optional<ReponseAvis> findByAvisId(Long avisId);

    /** Vérifie si une réponse existe déjà pour cet avis */
    boolean existsByAvisId(Long avisId);
}
