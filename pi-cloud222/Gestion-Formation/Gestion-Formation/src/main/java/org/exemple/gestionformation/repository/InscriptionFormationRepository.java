package org.exemple.gestionformation.repository;

import org.exemple.gestionformation.entity.InscriptionFormation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscriptionFormationRepository extends JpaRepository<InscriptionFormation, Long> {
    List<InscriptionFormation> findByFormationIdFormation(Long formationId);
    List<InscriptionFormation> findByUserId(Long userId);
}
