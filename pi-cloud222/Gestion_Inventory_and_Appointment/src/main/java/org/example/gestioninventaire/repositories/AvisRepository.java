package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.Avis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvisRepository extends JpaRepository<Avis, Long> {

    /** Tous les avis d'un vétérinaire, du plus récent au plus ancien */
    List<Avis> findByVeterinarianIdOrderByCreatedAtDesc(Long veterinarianId);

    /** Vérifie si un agriculteur a déjà évalué un vétérinaire */
    boolean existsByAgriculteurIdAndVeterinarianId(Long agriculteurId, Long veterinarianId);

    /** Trouve l'avis d'un agriculteur pour un vétérinaire donné */
    Optional<Avis> findByAgriculteurIdAndVeterinarianId(Long agriculteurId, Long veterinarianId);

    /** Calcule la moyenne des notes pour un vétérinaire */
    @Query("SELECT AVG(a.note) FROM Avis a WHERE a.veterinarianId = :vetId")
    Double findAverageNoteByVeterinarianId(@Param("vetId") Long vetId);

    /** Distribution des notes (1 à 5) pour un vétérinaire */
    @Query("SELECT a.note, COUNT(a) FROM Avis a WHERE a.veterinarianId = :vetId GROUP BY a.note ORDER BY a.note")
    List<Object[]> findNoteDistributionByVeterinarianId(@Param("vetId") Long vetId);

    /** Nombre total d'avis pour un vétérinaire */
    long countByVeterinarianId(Long veterinarianId);
}
