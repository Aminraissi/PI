package org.example.gestionevenement.Repositories;

import org.example.gestionevenement.entities.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface ReservationRepo extends JpaRepository<Reservation, Integer> {
    List<Reservation> findByEvenementId(int eventId);
    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.evenement WHERE r.id = :id")
    Optional<Reservation> findByIdWithEvent(@Param("id") int id);

}

