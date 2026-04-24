package org.exemple.paymentservice.repositories;

import org.exemple.paymentservice.entities.PaiementLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaiementLocationRepository extends JpaRepository<PaiementLocation, Long> {

    List<PaiementLocation> findByPropositionIdOrderByMoisNumeroAsc(Long propositionId);

    List<PaiementLocation> findByLocataireIdOrderByDateEcheanceAsc(Long locataireId);

    List<PaiementLocation> findByStatutAndDateEcheanceLessThanEqual(String statut, LocalDate date);

    boolean existsByPropositionId(Long propositionId);
}