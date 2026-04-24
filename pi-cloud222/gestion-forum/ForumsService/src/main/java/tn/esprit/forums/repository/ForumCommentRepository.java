package tn.esprit.forums.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.forums.model.ForumComment;

public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {

    long countByAuthorId(Long authorId);
}