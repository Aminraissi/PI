package org.exemple.gestionformation.repository;

import org.exemple.gestionformation.entity.Ressource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RessourceRepository extends JpaRepository<Ressource, Long> {
    List<Ressource> findByFormationIdFormation(Long formationId);
    List<Ressource> findByModuleIdModule(Long moduleId);
}
