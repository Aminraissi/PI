package org.example.servicepret.repositories;

import org.example.servicepret.entities.DemandePret;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandePretRepo extends JpaRepository<DemandePret,Long> {
    long countByService_Id(Long serviceId);
    List<DemandePret> findByService_Id(Long serviceId);
    List<DemandePret> findByAgriculteurIdOrderByDateDemandeDesc(Long agriculteurId);
}




