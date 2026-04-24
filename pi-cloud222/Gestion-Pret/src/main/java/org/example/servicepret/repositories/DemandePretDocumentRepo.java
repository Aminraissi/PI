package org.example.servicepret.repositories;


import org.example.servicepret.entities.DemandePretDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandePretDocumentRepo extends JpaRepository<DemandePretDocument, Long> {

    List<DemandePretDocument> findByDemandePret_Id(Long id);


}


