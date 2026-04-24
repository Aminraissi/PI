package tn.esprit.forums.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.forums.model.ForumGroup;

public interface ForumGroupRepository extends JpaRepository<ForumGroup, Long> {
    Optional<ForumGroup> findByNameIgnoreCase(String name);

    List<ForumGroup> findAllByOrderByIdDesc();
}