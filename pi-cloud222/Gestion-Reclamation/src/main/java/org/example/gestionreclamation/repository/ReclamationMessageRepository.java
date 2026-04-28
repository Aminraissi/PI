package org.example.gestionreclamation.repository;

import org.example.gestionreclamation.entity.ReclamationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReclamationMessageRepository extends JpaRepository<ReclamationMessage, Long> {
}
