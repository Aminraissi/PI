package tn.esprit.forums.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.forums.model.ForumPost;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    long countByAuthorId(Long authorId);

    @EntityGraph(attributePaths = {"replies"})
    List<ForumPost> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"replies"})
    List<ForumPost> findByGroupIdOrderByIdDesc(Long groupId);

    @Override
    @EntityGraph(attributePaths = {"replies"})
    Optional<ForumPost> findById(Long id);
}
