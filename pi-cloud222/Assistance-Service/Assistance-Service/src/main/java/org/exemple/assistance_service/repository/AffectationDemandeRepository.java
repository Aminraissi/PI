package org.exemple.assistance_service.repository;

import org.exemple.assistance_service.entity.AffectationDemande;
import org.exemple.assistance_service.enums.StatutAffectation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AffectationDemandeRepository extends JpaRepository<AffectationDemande, Long> {
    List<AffectationDemande> findByIngenieurId(Long ingenieurId);

    List<AffectationDemande> findByIngenieurIdAndStatut(Long ingenieurId, StatutAffectation statut);

    Optional<AffectationDemande> findByDemandeAssistance_IdDemande(Long demandeId);
}
