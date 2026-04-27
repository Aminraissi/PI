package org.example.gestionvente.Repositories;

import org.example.gestionvente.Entities.Location;
import org.example.gestionvente.Entities.ReservationVisite;
import org.example.gestionvente.Entities.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationVisiteRepo extends JpaRepository<ReservationVisite, Long> {

    List<ReservationVisite> findByLocationAndDateVisite(Location location, LocalDate dateVisite);

    List<ReservationVisite> findByLocationAndDateVisiteAndStatutIn(
            Location location,
            LocalDate dateVisite,
            List<StatutReservation> statuts
    );

    List<ReservationVisite> findByIdUser(Long idUser);

    boolean existsByLocationAndDateVisiteAndHeureDebutAndHeureFinAndStatutIn(
            Location location,
            LocalDate dateVisite,
            LocalTime heureDebut,
            LocalTime heureFin,
            List<StatutReservation> statuts
    );

    @Query("SELECT r FROM ReservationVisite r WHERE r.location IS NOT NULL AND r.location.idUser = :idUser")
    List<ReservationVisite> findByOwnerSafe(@Param("idUser") Long idUser);

    @Query("SELECT COUNT(r) > 0 FROM ReservationVisite r WHERE r.location.id = :locationId AND r.statut <> org.example.gestionvente.Entities.StatutReservation.ANNULEE")
    boolean existsActiveReservation(@Param("locationId") Long locationId);

    List<ReservationVisite> findByLocationId(Long locationId);

    @Query("""
    SELECT COUNT(r) > 0 
    FROM ReservationVisite r 
    WHERE r.location.id = :locationId 
    AND r.statut IN (
        org.example.gestionvente.Entities.StatutReservation.EN_ATTENTE,
        org.example.gestionvente.Entities.StatutReservation.CONFIRMEE
    )
    """)
    boolean existsBlockingReservation(@Param("locationId") Long locationId);

    @Modifying
    @Query("DELETE FROM ReservationVisite r WHERE r.location.id = :locationId")
    void deleteByLocationId(@Param("locationId") Long locationId);

    @Query("SELECT COUNT(r) > 0 FROM ReservationVisite r WHERE r.location.id = :locationId")
    boolean existsAnyReservation(@Param("locationId") Long locationId);

}