package org.example.gestionevenement.Repositories;

import org.example.gestionevenement.entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRepo extends JpaRepository<Event, Integer> {
    List<Event> findByIdOrganisateur(Long idOrganisateur);

    List<Event> findByGeolocatedFalseOrGeolocatedIsNull();

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
