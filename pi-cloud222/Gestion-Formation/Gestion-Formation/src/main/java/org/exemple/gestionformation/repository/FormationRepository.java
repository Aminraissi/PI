package org.exemple.gestionformation.repository;

import org.exemple.gestionformation.entity.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormationRepository extends JpaRepository<Formation, Long> {
}