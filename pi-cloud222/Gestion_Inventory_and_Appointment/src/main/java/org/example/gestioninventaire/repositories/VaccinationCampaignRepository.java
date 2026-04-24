package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.VaccinationCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaccinationCampaignRepository
        extends JpaRepository<VaccinationCampaign, Long> {
    List<VaccinationCampaign> findAll();
}
