package org.example.gestionevenement.Repositories;

import org.example.gestionevenement.entities.Event;
import org.example.gestionevenement.entities.StatutEvent;
import org.example.gestionevenement.entities.TypeEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventRepo extends JpaRepository<Event, Integer> {
    List<Event> findByIdOrganisateur(Long idOrganisateur);

    List<Event> findByIsValidTrue();
    @Query("""
SELECT e FROM Event e
WHERE e.isValid = true
AND (:type IS NULL OR e.type = :type)
AND (:region IS NULL OR e.region = :region)
""")
    Page<Event> findValidatedFiltered(
            @Param("type") TypeEvent type,
            @Param("region") String region,
            Pageable pageable
    );

    boolean existsByTitre(String titre);

    @Query(value = """
        SELECT
            e.id,
            e.capacite_max,
            e.date_debut,
            e.date_fin,
            e.description,
            e.image,
            e.inscrits,
            e.latitude,
            e.lieu,
            e.longitude,
            e.montant,
            e.region,
            e.statut,
            e.titre,
            e.type
        FROM event e
        WHERE e.statut NOT IN ('CANCELLED','COMPLETED')
        ORDER BY e.date_debut ASC
    """, nativeQuery = true)
    List<Object[]> findAllEventsMap();

}
