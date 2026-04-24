package org.example.servicepret.repositories;


import org.example.servicepret.entities.Echeance;
import org.example.servicepret.entities.StatutEcheance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EcheanceRepo extends JpaRepository<Echeance,Long> {

    Long id(Long id);
    List<Echeance> findByPretId(Long pretId);
    List<Echeance> findByStatutNot(StatutEcheance statut);
}
