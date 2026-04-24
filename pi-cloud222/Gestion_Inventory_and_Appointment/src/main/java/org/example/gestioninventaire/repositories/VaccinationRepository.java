package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {

    // JOIN FETCH product et animal pour éviter LazyInitializationException
    @Query("SELECT v FROM Vaccination v LEFT JOIN FETCH v.product LEFT JOIN FETCH v.animal WHERE v.campaign.id = :campaignId")
    List<Vaccination> findByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT v FROM Vaccination v LEFT JOIN FETCH v.product LEFT JOIN FETCH v.animal WHERE v.campaign.id = :campaignId ORDER BY v.id ASC")
    Optional<Vaccination> findFirstByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT v FROM Vaccination v LEFT JOIN FETCH v.product LEFT JOIN FETCH v.animal WHERE v.animal.id = :animalId")
    List<Vaccination> findByAnimalId(@Param("animalId") Long animalId);

    long countByCampaignId(Long campaignId);

    long countByCampaignIdAndStatus(Long campaignId, String status);

}
