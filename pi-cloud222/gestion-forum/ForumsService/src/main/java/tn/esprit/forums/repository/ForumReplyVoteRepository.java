package tn.esprit.forums.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.forums.model.ForumReplyVote;

public interface ForumReplyVoteRepository extends JpaRepository<ForumReplyVote, Long> {

    Optional<ForumReplyVote> findByReplyIdAndUserId(Long replyId, Long userId);

    @Query("select v from ForumReplyVote v where v.reply.post.id = :postId and v.userId = :userId")
    List<ForumReplyVote> findAllByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Query("select count(v) from ForumReplyVote v where v.reply.authorId = :authorId and v.voteType = :voteType")
    long countByReplyAuthorIdAndVoteType(@Param("authorId") Long authorId, @Param("voteType") String voteType);

    void deleteAllByReplyId(Long replyId);
}