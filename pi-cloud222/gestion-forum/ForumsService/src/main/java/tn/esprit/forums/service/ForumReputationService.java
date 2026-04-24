package tn.esprit.forums.service;

import org.springframework.stereotype.Service;
import tn.esprit.forums.repository.ForumCommentRepository;
import tn.esprit.forums.repository.ForumPostRepository;
import tn.esprit.forums.repository.ForumReplyRepository;
import tn.esprit.forums.repository.ForumReplyVoteRepository;

@Service
public class ForumReputationService {

    private final ForumPostRepository forumPostRepository;
    private final ForumReplyRepository forumReplyRepository;
    private final ForumCommentRepository forumCommentRepository;
    private final ForumReplyVoteRepository forumReplyVoteRepository;

    public ForumReputationService(
            ForumPostRepository forumPostRepository,
            ForumReplyRepository forumReplyRepository,
            ForumCommentRepository forumCommentRepository,
            ForumReplyVoteRepository forumReplyVoteRepository
    ) {
        this.forumPostRepository = forumPostRepository;
        this.forumReplyRepository = forumReplyRepository;
        this.forumCommentRepository = forumCommentRepository;
        this.forumReplyVoteRepository = forumReplyVoteRepository;
    }

    public int calculateReputation(Long userId) {
        long postPoints = forumPostRepository.countByAuthorId(userId) * 5L;
        long replyPoints = forumReplyRepository.countByAuthorId(userId) * 10L;
        long commentPoints = forumCommentRepository.countByAuthorId(userId) * 2L;
        long upvotePoints = forumReplyVoteRepository.countByReplyAuthorIdAndVoteType(userId, "UP") * 5L;
        long downvotePoints = forumReplyVoteRepository.countByReplyAuthorIdAndVoteType(userId, "DOWN") * -2L;
        long acceptedPoints = forumReplyRepository.countByAuthorIdAndIsAcceptedTrue(userId) * 15L;

        long reputation = postPoints + replyPoints + commentPoints + upvotePoints + downvotePoints + acceptedPoints;
        return (int) Math.max(0L, reputation);
    }

    public BadgeTier calculateBadgeTier(int reputation) {
        return BadgeTier.fromReputation(reputation);
    }
}