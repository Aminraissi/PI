package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.VaccinationCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationCampaignRepository
        extends JpaRepository<VaccinationCampaign, Long> {

    // ✅ Fix principal : récupérer uniquement les campagnes de l'agriculteur connecté
    List<VaccinationCampaign> findByOwnerId(Long ownerId);

    // Filtrer par ownerId ET statut (ex: PLANNED, IN_PROGRESS, COMPLETED)
    List<VaccinationCampaign> findByOwnerIdAndStatus(Long ownerId, String status);

    // Filtrer par ownerId ET espèce
    List<VaccinationCampaign> findByOwnerIdAndEspece(Long ownerId, String espece);

    // Campagnes d'un agriculteur prévues à une date précise
    List<VaccinationCampaign> findByOwnerIdAndPlannedDate(Long ownerId, LocalDate plannedDate);

    // Campagnes d'un agriculteur dans une plage de dates
    @Query("SELECT c FROM VaccinationCampaign c WHERE c.ownerId = :ownerId " +
            "AND c.plannedDate BETWEEN :start AND :end")
    List<VaccinationCampaign> findByOwnerIdAndDateRange(
            @Param("ownerId") Long ownerId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // Compter les campagnes d'un agriculteur par statut
    long countByOwnerIdAndStatus(Long ownerId, String status);

    // Campagnes liées à un produit vaccin spécifique
    List<VaccinationCampaign> findByOwnerIdAndProductId(Long ownerId, Long productId);
}