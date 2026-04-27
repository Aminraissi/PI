package org.example.gestionreclamation.repository;

import org.example.gestionreclamation.entity.Reclamation;
import org.example.gestionreclamation.enums.ReclamationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    List<Reclamation> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Reclamation> findByStatusOrderByCreatedAtDesc(ReclamationStatus status);
    List<Reclamation> findAllByOrderByCreatedAtDesc();
}
