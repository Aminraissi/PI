package tn.esprit.forums.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.forums.model.ForumReply;

public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {

    Optional<ForumReply> findByIdAndPostId(Long id, Long postId);
    
    long countByAuthorId(Long authorId);
    
    long countByAuthorIdAndIsAcceptedTrue(Long authorId);
}
