package org.example.servicepret.repositories;

import org.example.servicepret.entities.ServicePret;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepo extends JpaRepository <ServicePret,Long> {

    List<ServicePret> findByAgentId(Long agentId);


}
