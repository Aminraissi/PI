package org.example.servicepret.repositories;

import org.example.servicepret.entities.Pret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PretRepo extends JpaRepository<Pret, Long> {
    Optional<Pret> findByDemande_Id(Long demandeId);

    List<Pret> findByDemande_AgriculteurId(Long agriculteurId);
}
