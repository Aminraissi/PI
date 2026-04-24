package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.CommentaireAvis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentaireAvisRepository extends JpaRepository<CommentaireAvis, Long> {

    /** Tous les commentaires d'un avis, du plus récent au plus ancien */
    List<CommentaireAvis> findByAvisIdOrderByCreatedAtAsc(Long avisId);
}
