package tn.esprit.forums.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying
    @Query("update ForumPost post set post.views = post.views + 1 where post.id = :postId")
    int incrementViewsById(@Param("postId") Long postId);
}
