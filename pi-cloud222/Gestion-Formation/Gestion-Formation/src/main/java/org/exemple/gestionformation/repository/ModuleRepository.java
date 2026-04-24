package org.exemple.gestionformation.repository;

import org.exemple.gestionformation.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, Long> {
    List<Module> findByFormationIdFormation(Long formationId);
}