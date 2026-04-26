package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.VetPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VetPostRepository extends JpaRepository<VetPost, Long> {
    List<VetPost> findByVeterinarianIdOrderByCreatedAtDesc(Long veterinarianId);
}