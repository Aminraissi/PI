package org.exemple.assistance_service.repository;

import org.exemple.assistance_service.entity.ReponseIA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReponseIARepository extends JpaRepository<ReponseIA, Long> {
    boolean existsByDemandeAssistance_IdDemande(Long demandeId);

    Optional<ReponseIA> findByDemandeAssistance_IdDemande(Long demandeId);
}
