package org.exemple.assistance_service.repository;

import org.exemple.assistance_service.entity.DemandeAssistance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeAssistanceRepository extends JpaRepository<DemandeAssistance, Long> {
    List<DemandeAssistance> findByUserId(Long userId);
}