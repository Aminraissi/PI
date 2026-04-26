package org.example.servicepret.repositories;

import org.example.servicepret.entities.Contrat;
import org.example.servicepret.entities.StatutContrat;
import org.example.servicepret.entities.ValidationAdminStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContratRepo extends JpaRepository<Contrat, Long> {

    Contrat findByDemandeId(Long demandeId);

    @Query("SELECT c FROM Contrat c JOIN FETCH c.demande d WHERE c.id = :id")
    Contrat findByIdWithDetails(@Param("id") Long id);


    @Query("SELECT c FROM Contrat c LEFT JOIN FETCH c.demande d WHERE c.statutContrat = :statut AND c.validationAdmin = :validation")
    List<Contrat> findByStatutContratAndValidationAdmin(
            @Param("statut") StatutContrat statut,
            @Param("validation") ValidationAdminStatus validationAdmin
    );

    @Query("SELECT c FROM Contrat c JOIN FETCH c.demande d WHERE c.validationAdmin = :validation")
    List<Contrat> findByValidationAdmin(@Param("validation") ValidationAdminStatus validationAdmin);

    @Query("SELECT c FROM Contrat c JOIN FETCH c.demande d")
    List<Contrat> findAllWithDetails();

    List<Contrat> findByStatutContrat(StatutContrat statut);
}