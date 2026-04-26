package org.example.gestionvente.Repositories;

import org.example.gestionvente.Entities.PropositionLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropositionLocationRepo extends JpaRepository<PropositionLocation, Long> {

    List<PropositionLocation> findByLocataireId(Long locataireId);

    List<PropositionLocation> findByAgriculteurId(Long agriculteurId);

    boolean existsByReservationId(Long reservationId);

    List<PropositionLocation> findByLocationId(Long locationId);
    List<PropositionLocation> findByLocataireIdAndLocationId(Long locataireId, Long locationId);
    boolean existsByLocationId(Long locationId);

    boolean existsByLocationIdAndStatutIgnoreCase(Long locationId, String statut);
}