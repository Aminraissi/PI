package org.example.servicepret.repositories;

import org.example.servicepret.entities.DocumentAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAccessLogRepo extends JpaRepository<DocumentAccessLog, Long> {

}
