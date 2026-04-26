package org.exemple.gestionformation.repository;

import org.exemple.gestionformation.entity.LeconCommentaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeconCommentaireRepository extends JpaRepository<LeconCommentaire, Long> {
    List<LeconCommentaire> findByLeconIdLeconOrderByDateCreationAsc(Long leconId);
}
