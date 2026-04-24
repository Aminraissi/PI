package org.example.gestionvente.Repositories;

import org.example.gestionvente.Entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepo extends JpaRepository<Review, Long> {

    List<Review> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);

    Optional<Review> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    long countByTargetTypeAndTargetId(String targetType, Long targetId);

    List<Review> findByUserId(Long userId);
}