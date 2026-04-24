package org.example.servicepret.repositories;

import org.example.servicepret.entities.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContratRepo extends JpaRepository<Contrat,Long> {
    Contrat findByDemandeId(Long demandeId);

    @Query("SELECT c FROM Contrat c " +
            "JOIN FETCH c.demande d " +
            "WHERE c.id = :id")
    Contrat findByIdWithDetails(@Param("id") Long id);
}
