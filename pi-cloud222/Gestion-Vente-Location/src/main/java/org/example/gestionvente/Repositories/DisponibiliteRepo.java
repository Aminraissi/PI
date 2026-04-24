package org.example.gestionvente.Repositories;

import org.example.gestionvente.Entities.Disponibilite;
import org.example.gestionvente.Entities.JourSemaine;
import org.example.gestionvente.Entities.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DisponibiliteRepo extends JpaRepository<Disponibilite, Long> {
    boolean existsByLocationAndJourSemaine(Location location, JourSemaine jourSemaine);

    @Query("SELECT d FROM Disponibilite d WHERE d.location = :location AND d.jourSemaine = :jour")
    List<Disponibilite> findByLocationAndJourSemaine(@Param("location") Location location,
                                                     @Param("jour") JourSemaine jour);
    Optional<Disponibilite> findByLocationAndJourSemaineAndHeureDebutAndHeureFinAndEstActive(
            Location location,
            JourSemaine jourSemaine,
            LocalTime heureDebut,
            LocalTime heureFin,
            boolean estActive
    );

    List<Disponibilite> findByLocationId(Long locationId);
    void deleteByLocationId(Long locationId);
}