package tn.esprit.forums.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.forums.dto.CreateCommentRequest;
import tn.esprit.forums.dto.AiDuplicateCandidateResponse;
import tn.esprit.forums.dto.AiModerationAnalysisRequest;
import tn.esprit.forums.dto.AiModerationAnalysisResponse;
import tn.esprit.forums.dto.CreateGroupRequest;
import tn.esprit.forums.dto.CreatePostRequest;
import tn.esprit.forums.dto.CreateReplyRequest;
import tn.esprit.forums.dto.CreateReportRequest;
import tn.esprit.forums.model.ForumComment;
import tn.esprit.forums.model.ForumGroup;
import tn.esprit.forums.model.ForumPost;
import tn.esprit.forums.model.ForumReport;
import tn.esprit.forums.model.ForumReply;
import tn.esprit.forums.model.ForumUser;
import tn.esprit.forums.repository.ForumGroupRepository;
import tn.esprit.forums.repository.ForumPostRepository;
import tn.esprit.forums.repository.ForumReportRepository;
import tn.esprit.forums.repository.ForumReplyRepository;
import tn.esprit.forums.repository.ForumReplyVoteRepository;
import tn.esprit.forums.repository.ForumCommentRepository;

@Service
public class ForumsService {
    private static final Logger LOG = LoggerFactory.getLogger(ForumsService.class);

    public static final Long AI_ASSISTANT_USER_ID = -100L;
    private static final String REPORT_STATUS_PENDING = "PENDING";
    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("(?is)<script.*?>.*?</script>");
    private static final Pattern INLINE_EVENT_PATTERN = Pattern.compile("(?i)\\son[a-z]+\\s*=\\s*\"[^\"]*\"");
    private static final Pattern JS_URI_PATTERN = Pattern.compile("(?i)javascript:");

    private final ForumPostRepository forumPostRepository;
    private final ForumGroupRepository forumGroupRepository;
    private final ForumReplyRepository forumReplyRepository;
    private final ForumReplyVoteRepository forumReplyVoteRepository;
    private final ForumCommentRepository forumCommentRepository;
    private final ForumReportRepository forumReportRepository;
    private final UserServiceClient userServiceClient;
    private final ForumReputationService reputationService;
    private final ForumAiSuggestionService forumAiSuggestionService;
    private final ForumImageModerationService forumImageModerationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${forums.moderation.report-threshold.post:5}")
    private int postReportThreshold;

    @Value("${forums.moderation.report-threshold.reply:4}")
    private int replyReportThreshold;

    @Value("${forums.moderation.report-threshold.comment:3}")
    private int commentReportThreshold;

    public ForumsService(
            ForumPostRepository forumPostRepository,
            ForumGroupRepository forumGroupRepository,
            ForumReplyRepository forumReplyRepository,
            ForumReplyVoteRepository forumReplyVoteRepository,
            ForumCommentRepository forumCommentRepository,
            ForumReportRepository forumReportRepository,
            UserServiceClient userServiceClient,
            ForumReputationService reputationService,
            ForumAiSuggestionService forumAiSuggestionService,
            ForumImageModerationService forumImageModerationService
    ) {
        this.forumPostRepository = forumPostRepository;
        this.forumGroupRepository = forumGroupRepository;
        this.forumReplyRepository = forumReplyRepository;
        this.forumReplyVoteRepository = forumReplyVoteRepository;
        this.forumCommentRepository = forumCommentRepository;
        this.forumReportRepository = forumReportRepository;
        this.userServiceClient = userServiceClient;
        this.reputationService = reputationService;
        this.forumAiSuggestionService = forumAiSuggestionService;
        this.forumImageModerationService = forumImageModerationService;
    }

    @Transactional(readOnly = true)
    public List<ForumPost> getPosts(Long currentUserId) {
        return getPosts(currentUserId, "newest", "desc", null, List.of(), null);
    }

    public List<ForumPost> getPosts(Long currentUserId, String sortBy, String sortDirection, String searchTerm, List<String> tags, Long groupId) {
        boolean adminView = isAdminView(currentUserId);
        List<ForumPost> posts = groupId == null
                ? forumPostRepository.findAllByOrderByIdDesc()
                : forumPostRepository.findByGroupIdOrderByIdDesc(groupId);

        posts.forEach(post -> {
            post.getReplies().forEach(reply -> reply.getComments().size());
        });

        hydrateReportState(posts, currentUserId);
        posts = applySearchAndTagFilters(posts, searchTerm, tags);
        posts = applySorting(posts, sortBy, sortDirection, adminView);

        posts.forEach(post -> {
            enforceModerationVisibility(post, adminView);
            enforceDeletedContentPolicy(post);
        });
        applyCurrentUserVotes(posts, currentUserId);
        return posts;
    }

    @Transactional(readOnly = true)
    public ForumPost getPostById(Long postId, Long currentUserId) {
        LOG.info("ForumsService.getPostById start postId={} currentUserId={}", postId, currentUserId);
        boolean adminView = isAdminView(currentUserId);
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + postId));

        LOG.info("ForumsService.getPostById found post id={} title='{}' replies={}", post.getId(), post.getTitle(), post.getReplies().size());
        initializeMediaCollections(post);
        post.getReplies().forEach(reply -> reply.getComments().size());
        LOG.info("ForumsService.getPostById comments initialized for postId={}", postId);
        hydrateReportState(List.of(post), currentUserId);
        LOG.info("ForumsService.getPostById report state hydrated for postId={}", postId);
        applyCurrentUserVotes(List.of(post), currentUserId);
        LOG.info("ForumsService.getPostById user votes applied for postId={}", postId);
        entityManager.detach(post);
        LOG.info("ForumsService.getPostById entity detached for postId={}", postId);

        // Apply response masking after persistence updates so hidden placeholders are never stored in DB.
        enforceModerationVisibility(post, adminView);
        enforceDeletedContentPolicy(post);
        LOG.info("ForumsService.getPostById success postId={} mediaCount={} deleted={} hidden={}",
                postId,
                post.getMediaUrls() == null ? 0 : post.getMediaUrls().size(),
                post.isDeleted(),
                post.isHiddenByReports());
        return post;
    }

    @Transactional
    public ForumPost createPost(CreatePostRequest request) {
        Long authorId = request.authorId() == null ? 2L : request.authorId();
        ensureUserExists(authorId);
        boolean shouldGenerateAiReply = !Boolean.FALSE.equals(request.generateAiReply());
        String safeTitle = request.title().trim();
        String safeContent = sanitizeRichText(request.content());
        List<String> safeTags = sanitizeTags(request.tags());
        List<String> safeMediaUrls = sanitizeMediaUrls(request.mediaUrls());
        ForumGroup selectedGroup = null;

        if (request.groupId() != null) {
            selectedGroup = forumGroupRepository.findById(request.groupId())
                    .orElseThrow(() -> new NoSuchElementException("Group not found: " + request.groupId()));
        }

        ForumAiSuggestionService.PostPublicationReview publicationReview = forumAiSuggestionService.reviewPostForPublishing(
                safeTitle,
                safeContent,
                safeTags,
                selectedGroup == null ? null : selectedGroup.getName(),
                selectedGroup == null ? List.of() : selectedGroup.getRules()
        );
        if (!publicationReview.allowed()) {
            throw new IllegalArgumentException(publicationReview.reason());
        }

        ForumImageModerationService.ModerationOutcome mediaModerationOutcome =
                forumImageModerationService.moderatePostMedia(safeMediaUrls);
        if (mediaModerationOutcome.status() == ForumImageModerationService.ModerationStatus.REJECTED) {
            throw new IllegalArgumentException(mediaModerationOutcome.reason());
        }

        ForumPost post = new ForumPost();
        post.setTitle(safeTitle);
        post.setContent(safeContent);
        post.setTags(safeTags);
        post.setMediaUrls(safeMediaUrls);
        post.setMediaApproved(mediaModerationOutcome.status() != ForumImageModerationService.ModerationStatus.REVIEW_REQUIRED);
        post.setMediaPendingReview(!safeMediaUrls.isEmpty()
                && mediaModerationOutcome.status() == ForumImageModerationService.ModerationStatus.REVIEW_REQUIRED);
        post.setAuthorId(authorId);
        post.setGroupId(request.groupId());
        post.setCreatedAt(nowIso());
        post.setViews(0);

        ForumPost saved = forumPostRepository.save(post);

        if (shouldGenerateAiReply) {
            forumAiSuggestionService.generateSuggestion(
                            saved.getTitle(),
                            saved.getContent(),
                            saved.getTags(),
                            selectedGroup == null ? null : selectedGroup.getName(),
                            selectedGroup == null ? null : selectedGroup.getDescription(),
                            selectedGroup == null ? List.of() : selectedGroup.getFocusTags(),
                            selectedGroup == null ? List.of() : selectedGroup.getRules()
                    )
                    .ifPresent(aiText -> {
                        ForumReply aiReply = new ForumReply();
                        aiReply.setAuthorId(AI_ASSISTANT_USER_ID);
                        aiReply.setContent(aiText);
                        aiReply.setUpvotes(0);
                        aiReply.setDownvotes(0);
                        aiReply.setAccepted(false);
                        aiReply.setCreatedAt(nowIso());
                        saved.addReply(aiReply);
                        forumPostRepository.save(saved);
                    });
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<String> suggestTags(String title, String content, List<String> tags, Long groupId) {
        ForumGroup selectedGroup = null;
        if (groupId != null) {
            selectedGroup = forumGroupRepository.findById(groupId)
                    .orElse(null);
        }

        return forumAiSuggestionService.recommendTags(
                title,
                content,
                tags,
                selectedGroup == null ? null : selectedGroup.getName(),
                selectedGroup == null ? null : selectedGroup.getDescription(),
                selectedGroup == null ? List.of() : selectedGroup.getFocusTags()
        );
    }

    @Transactional(readOnly = true)
    public String improveReplyDraft(String draft, String postTitle, String postContent, Long groupId) {
        ForumGroup selectedGroup = null;
        if (groupId != null) {
            selectedGroup = forumGroupRepository.findById(groupId).orElse(null);
        }

        return forumAiSuggestionService.improveReplyDraft(
                        draft,
                        postTitle,
                        postContent,
                        selectedGroup == null ? null : selectedGroup.getName(),
                        selectedGroup == null ? null : selectedGroup.getDescription(),
                        selectedGroup == null ? List.of() : selectedGroup.getFocusTags(),
                        selectedGroup == null ? List.of() : selectedGroup.getRules()
                )
                .orElseGet(() -> draft == null ? "" : draft.trim());
    }

    @Transactional(readOnly = true)
    public List<AiDuplicateCandidateResponse> findDuplicateCandidates(String title, String content, List<String> tags, Long groupId) {
        ForumGroup selectedGroup = null;
        if (groupId != null) {
            selectedGroup = forumGroupRepository.findById(groupId).orElse(null);
        }

        List<ForumPost> candidates = groupId == null
                ? forumPostRepository.findAllByOrderByIdDesc()
                : forumPostRepository.findByGroupIdOrderByIdDesc(groupId);

        List<ForumAiSuggestionService.DuplicateCandidateData> candidateData = candidates.stream()
                .filter(post -> post.getTitle() != null && post.getContent() != null)
                .map(post -> new ForumAiSuggestionService.DuplicateCandidateData(
                        post.getId(),
                        post.getTitle(),
                        post.getTags(),
                        post.getReplies() == null ? 0 : (int) post.getReplies().stream().filter(reply -> !reply.isDeleted()).count(),
                        post.getViews(),
                        post.getCreatedAt()
                ))
                .limit(20)
                .toList();

        return forumAiSuggestionService.rankDuplicateCandidates(
                        title,
                        content,
                        tags,
                        selectedGroup == null ? null : selectedGroup.getName(),
                        selectedGroup == null ? null : selectedGroup.getDescription(),
                        selectedGroup == null ? List.of() : selectedGroup.getFocusTags(),
                        candidateData
                )
                .stream()
                .map(match -> candidateData.stream()
                        .filter(candidate -> candidate.id().equals(match.id()))
                        .findFirst()
                        .map(candidate -> new AiDuplicateCandidateResponse(
                                candidate.id(),
                                candidate.title(),
                                match.score(),
                                candidate.replies(),
                                candidate.views(),
                                match.reason()
                        ))
                        .orElse(null))
                .filter(candidate -> candidate != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AiModerationAnalysisResponse> analyzeModerationCase(AiModerationAnalysisRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        ForumGroup selectedGroup = null;
        if (request.groupId() != null) {
            selectedGroup = forumGroupRepository.findById(request.groupId()).orElse(null);
        }

        return forumAiSuggestionService.analyzeModerationCase(
                request.title(),
                request.content(),
                request.tags(),
                selectedGroup == null ? null : selectedGroup.getName(),
                selectedGroup == null ? null : selectedGroup.getDescription(),
                selectedGroup == null ? List.of() : selectedGroup.getFocusTags(),
                selectedGroup == null ? List.of() : selectedGroup.getRules()
        );
    }

    @Transactional
    public void addReply(Long postId, CreateReplyRequest request) {
        Long authorId = request.authorId() == null ? 2L : request.authorId();
        ensureUserExists(authorId);

        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + postId));

        if (post.isDeleted()) {
            throw new IllegalArgumentException("Post has been removed");
        }

        ForumAiSuggestionService.PostPublicationReview publicationReview = forumAiSuggestionService.reviewContentForPublishing(
                post.getTitle(),
                sanitizeRichText(request.content()),
                post.getTags(),
                resolveGroupName(post.getGroupId()),
                resolveGroupRules(post.getGroupId()),
                "reply"
        );
        if (!publicationReview.allowed()) {
            throw new IllegalArgumentException(publicationReview.reason());
        }

        List<String> safeMediaUrls = sanitizeMediaUrls(request.mediaUrls());
        ForumImageModerationService.ModerationOutcome mediaModerationOutcome =
                forumImageModerationService.moderatePostMedia(safeMediaUrls);
        if (mediaModerationOutcome.status() == ForumImageModerationService.ModerationStatus.REJECTED) {
            throw new IllegalArgumentException(mediaModerationOutcome.reason());
        }

        ForumReply newReply = new ForumReply();
        newReply.setAuthorId(authorId);
        newReply.setContent(sanitizeRichText(request.content()));
        newReply.setMediaUrls(safeMediaUrls);
        newReply.setMediaApproved(mediaModerationOutcome.status() != ForumImageModerationService.ModerationStatus.REVIEW_REQUIRED);
        newReply.setMediaPendingReview(!safeMediaUrls.isEmpty()
                && mediaModerationOutcome.status() == ForumImageModerationService.ModerationStatus.REVIEW_REQUIRED);
        newReply.setUpvotes(0);
        newReply.setDownvotes(0);
        newReply.setAccepted(false);
        newReply.setCreatedAt(nowIso());
        post.addReply(newReply);

        forumPostRepository.save(post);
        applyBestAnswerRule(post);
    }

    @Transactional
    public void voteReply(Long postId, Long replyId, String voteType) {
        throw new UnsupportedOperationException("Use voteReply with user id");
    }

    @Transactional
    public void voteReply(Long postId, Long replyId, String voteType, Long userId) {
        ensureUserExists(userId);

        ForumReply reply = forumReplyRepository.findByIdAndPostId(replyId, postId)
                .orElseThrow(() -> new NoSuchElementException("Reply not found: " + replyId));
        if (reply.isDeleted()) {
            throw new IllegalArgumentException("Reply has been removed");
        }

        String normalizedVote = normalizeVoteType(voteType);
        var existingVote = forumReplyVoteRepository.findByReplyIdAndUserId(replyId, userId);

        if (existingVote.isPresent()) {
            String currentVoteType = existingVote.get().getVoteType();
            if (currentVoteType.equals(normalizedVote)) {
                if ("UP".equals(currentVoteType)) {
                    reply.setUpvotes(Math.max(0, reply.getUpvotes() - 1));
                } else {
                    reply.setDownvotes(Math.max(0, reply.getDownvotes() - 1));
                }
                forumReplyVoteRepository.delete(existingVote.get());
                forumReplyRepository.save(reply);
                applyBestAnswerRule(reply.getPost());
                return;
            }

            if ("UP".equals(currentVoteType)) {
                reply.setUpvotes(Math.max(0, reply.getUpvotes() - 1));
            } else {
                reply.setDownvotes(Math.max(0, reply.getDownvotes() - 1));
            }

            if ("UP".equals(normalizedVote)) {
                reply.setUpvotes(reply.getUpvotes() + 1);
            } else {
                reply.setDownvotes(reply.getDownvotes() + 1);
            }

            existingVote.get().setVoteType(normalizedVote);
            existingVote.get().setUpdatedAt(nowIso());
            forumReplyVoteRepository.save(existingVote.get());
        } else {
            if ("UP".equals(normalizedVote)) {
                reply.setUpvotes(reply.getUpvotes() + 1);
            } else {
                reply.setDownvotes(reply.getDownvotes() + 1);
            }

            tn.esprit.forums.model.ForumReplyVote replyVote = new tn.esprit.forums.model.ForumReplyVote();
            replyVote.setReply(reply);
            replyVote.setUserId(userId);
            replyVote.setVoteType(normalizedVote);
            replyVote.setCreatedAt(nowIso());
            replyVote.setUpdatedAt(nowIso());
            forumReplyVoteRepository.save(replyVote);
        }

        forumReplyRepository.save(reply);
        applyBestAnswerRule(reply.getPost());
    }

    private String normalizeVoteType(String voteType) {
        if ("up".equalsIgnoreCase(voteType)) {
            return "UP";
        }
        if ("down".equalsIgnoreCase(voteType)) {
            return "DOWN";
        }
        throw new IllegalArgumentException("voteType must be 'up' or 'down'");
    }

    @Transactional
    public void addComment(Long postId, Long replyId, CreateCommentRequest request) {
        Long authorId = request.authorId() == null ? 2L : request.authorId();
        ensureUserExists(authorId);

        ForumReply reply = forumReplyRepository.findByIdAndPostId(replyId, postId)
                .orElseThrow(() -> new NoSuchElementException("Reply not found: " + replyId));
        if (reply.isDeleted()) {
            throw new IllegalArgumentException("Reply has been removed");
        }

        ForumAiSuggestionService.PostPublicationReview publicationReview = forumAiSuggestionService.reviewContentForPublishing(
                reply.getPost() == null ? "" : reply.getPost().getTitle(),
                sanitizeRichText(request.content()),
                reply.getPost() == null ? List.of() : reply.getPost().getTags(),
                reply.getPost() == null ? null : resolveGroupName(reply.getPost().getGroupId()),
                reply.getPost() == null ? List.of() : resolveGroupRules(reply.getPost().getGroupId()),
                "comment"
        );
        if (!publicationReview.allowed()) {
            throw new IllegalArgumentException(publicationReview.reason());
        }

        ForumComment comment = new ForumComment();
        comment.setAuthorId(authorId);
        comment.setContent(sanitizeRichText(request.content()));
        comment.setCreatedAt(nowIso());
        reply.addComment(comment);

        forumReplyRepository.save(reply);
    }

    @Transactional
    public void deleteReply(Long postId, Long replyId, Long userId) {
        ForumReply reply = forumReplyRepository.findByIdAndPostId(replyId, postId)
                .orElseThrow(() -> new NoSuchElementException("Reply not found: " + replyId));

        ensureOwnerOrAdmin(userId, reply.getAuthorId());

        if (reply.isDeleted()) {
            return;
        }

        reply.setDeleted(true);
        reply.setDeletedByAdmin(isAdmin(userId));
        reply.setDeletedAt(nowIso());
        reply.setContent(reply.isDeletedByAdmin()
            ? "This reply has been removed by an administrator."
            : "This reply has been removed by the user.");
        reply.setAccepted(false);
        reply.setUpvotes(0);
        reply.setDownvotes(0);
        reply.getComments().clear();
        forumReplyVoteRepository.deleteAllByReplyId(replyId);

        forumReplyRepository.save(reply);
        applyBestAnswerRule(reply.getPost());
    }

    @Transactional
    public void deleteComment(Long postId, Long replyId, Long commentId, Long userId) {
        ensureUserExists(userId);

        ForumReply reply = forumReplyRepository.findByIdAndPostId(replyId, postId)
                .orElseThrow(() -> new NoSuchElementException("Reply not found: " + replyId));

        ForumComment comment = reply.getComments().stream()
                .filter(item -> item.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Comment not found: " + commentId));

        ensureOwnerOrAdmin(userId, comment.getAuthorId());

        if (comment.isDeleted()) {
            return;
        }

        comment.setDeleted(true);
        comment.setDeletedByAdmin(isAdmin(userId));
        comment.setDeletedAt(nowIso());
        comment.setContent(comment.isDeletedByAdmin()
            ? "This comment has been removed by an administrator."
            : "This comment has been removed by the user.");
        forumCommentRepository.save(comment);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        ensureUserExists(userId);

        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + postId));
        if (post.isDeleted()) {
            return;
        }

        ensureOwnerOrAdmin(userId, post.getAuthorId());

        if (post.getReplies().isEmpty()) {
            forumPostRepository.delete(post);
            return;
        }

        post.setDeleted(true);
        post.setDeletedByAdmin(isAdmin(userId));
        post.setDeletedAt(nowIso());
        post.setTitle("Post was removed");
        post.setContent(post.isDeletedByAdmin()
            ? "This post was removed by an administrator."
            : "This post was removed by the user.");

        // AI insight should disappear when the parent post is deleted.
        for (ForumReply reply : post.getReplies()) {
            if (!AI_ASSISTANT_USER_ID.equals(reply.getAuthorId())) {
                continue;
            }
            reply.setDeleted(true);
            reply.setDeletedByAdmin(post.isDeletedByAdmin());
            reply.setDeletedAt(nowIso());
            reply.setContent("This assistant reply was removed because the original post was deleted.");
            reply.setAccepted(false);
            reply.setUpvotes(0);
            reply.setDownvotes(0);
            reply.getComments().clear();
        }

        forumPostRepository.save(post);
    }

    @Transactional
    public Map<String, Object> reportPost(Long postId, Long userId, CreateReportRequest request) {
        ensureUserExists(userId);

        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + postId));

        if (post.isDeleted() || post.isHiddenByReports()) {
            throw new IllegalArgumentException("Post has already been removed");
        }

        ensureTargetNotAlreadyReportedByUser("POST", post.getId(), userId);

        saveReport("POST", post.getId(), post.getId(), null, null, userId, request);
        post.setReportCount(post.getReportCount() + 1);
        if (post.getReportCount() >= postReportThreshold && !post.isHiddenByReports()) {
            post.setHiddenByReports(true);
            post.setHiddenAt(nowIso());
        }
        forumPostRepository.save(post);

        return Map.of(
                "targetType", "POST",
                "targetId", post.getId(),
                "reportCount", post.getReportCount(),
                "hidden", post.isHiddenByReports(),
                "threshold", postReportThreshold
        );
    }

    @Transactional
    public Map<String, Object> reportReply(Long postId, Long replyId, Long userId, CreateReportRequest request) {
        ensureUserExists(userId);

        ForumReply reply = forumReplyRepository.findByIdAndPostId(replyId, postId)
                .orElseThrow(() -> new NoSuchElementException("Reply not found: " + replyId));

        if (reply.isDeleted() || reply.isHiddenByReports()) {
            throw new IllegalArgumentException("Reply has already been removed");
        }

        ensureTargetNotAlreadyReportedByUser("REPLY", reply.getId(), userId);

        saveReport("REPLY", reply.getId(), postId, reply.getId(), null, userId, request);
        reply.setReportCount(reply.getReportCount() + 1);
        if (reply.getReportCount() >= replyReportThreshold && !reply.isHiddenByReports()) {
            reply.setHiddenByReports(true);
            reply.setHiddenAt(nowIso());
        }
        forumReplyRepository.save(reply);

        return Map.of(
                "targetType", "REPLY",
                "targetId", reply.getId(),
                "reportCount", reply.getReportCount(),
                "hidden", reply.isHiddenByReports(),
                "threshold", replyReportThreshold
        );
    }

    @Transactional
    public Map<String, Object> reportComment(Long postId, Long replyId, Long commentId, Long userId, CreateReportRequest request) {
        ensureUserExists(userId);

        ForumReply reply = forumReplyRepository.findByIdAndPostId(replyId, postId)
                .orElseThrow(() -> new NoSuchElementException("Reply not found: " + replyId));

        ForumComment comment = reply.getComments().stream()
                .filter(item -> item.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Comment not found: " + commentId));

        if (comment.isDeleted() || comment.isHiddenByReports()) {
            throw new IllegalArgumentException("Comment has already been removed");
        }

        ensureTargetNotAlreadyReportedByUser("COMMENT", comment.getId(), userId);

        saveReport("COMMENT", comment.getId(), postId, replyId, comment.getId(), userId, request);
        comment.setReportCount(comment.getReportCount() + 1);
        if (comment.getReportCount() >= commentReportThreshold && !comment.isHiddenByReports()) {
            comment.setHiddenByReports(true);
            comment.setHiddenAt(nowIso());
        }
        forumCommentRepository.save(comment);

        return Map.of(
                "targetType", "COMMENT",
                "targetId", comment.getId(),
                "reportCount", comment.getReportCount(),
                "hidden", comment.isHiddenByReports(),
                "threshold", commentReportThreshold
        );
    }

    @Transactional(readOnly = true)
    public List<ForumReport> getReportsForTarget(String targetType, Long targetId, Long adminUserId) {
        ensureAdmin(adminUserId);
        if ("POST".equalsIgnoreCase(targetType)) {
            return forumReportRepository.findAll().stream()
                    .filter(report -> targetId.equals(report.getPostId()) || ("POST".equalsIgnoreCase(report.getTargetType()) && targetId.equals(report.getTargetId())))
                    .sorted((left, right) -> right.getId().compareTo(left.getId()))
                    .toList();
        }

        return forumReportRepository.findByTargetTypeAndTargetIdOrderByIdDesc(targetType.toUpperCase(), targetId);
    }

    @Transactional(readOnly = true)
    public List<ForumReport> getAllReports(Long adminUserId, String status) {
        ensureAdmin(adminUserId);

        if (status != null && !status.isBlank()) {
            return forumReportRepository.findByStatusOrderByIdDesc(status.trim().toUpperCase());
        }

        return forumReportRepository.findAll().stream()
                .sorted((left, right) -> right.getId().compareTo(left.getId()))
                .toList();
    }

    @Transactional
    public void approveReportedPost(Long postId, Long adminUserId) {
        ensureAdmin(adminUserId);
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + postId));
        post.setHiddenByReports(false);
        post.setHiddenAt(null);
        post.setReportCount(0);
        forumPostRepository.save(post);
        markReportsReviewed("POST", postId, adminUserId, "APPROVED");
    }

    @Transactional
    public void rejectReportedPost(Long postId, Long adminUserId) {
        ensureAdmin(adminUserId);
        deletePost(postId, adminUserId);
        markReportsReviewed("POST", postId, adminUserId, "REJECTED");
    }

    @Transactional
    public void approvePostMedia(Long postId, Long adminUserId) {
        ensureAdmin(adminUserId);
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + postId));

        if (post.getMediaUrls() == null || post.getMediaUrls().isEmpty()) {
            return;
        }

        post.setMediaApproved(true);
        post.setMediaPendingReview(false);
        forumPostRepository.save(post);
    }

    @Transactional
    public void rejectPostMedia(Long postId, Long adminUserId) {
        ensureAdmin(adminUserId);
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + postId));

        post.setMediaUrls(new ArrayList<>());
        post.setMediaApproved(true);
        post.setMediaPendingReview(false);
        forumPostRepository.save(post);
    }

    @Transactional
    public void approveReplyMedia(Long postId, Long replyId, Long adminUserId) {
        ensureAdmin(adminUserId);
        ForumReply reply = forumReplyRepository.findByIdAndPostId(replyId, postId)
                .orElseThrow(() -> new NoSuchElementException("Reply not found: " + replyId));

        if (reply.getMediaUrls() == null || reply.getMediaUrls().isEmpty()) {
            return;
        }

        reply.setMediaApproved(true);
        reply.setMediaPendingReview(false);
        forumReplyRepository.save(reply);
    }

    @Transactional
    public void rejectReplyMedia(Long postId, Long replyId, Long adminUserId) {
        ensureAdmin(adminUserId);
        ForumReply reply = forumReplyRepository.findByIdAndPostId(replyId, postId)
                .orElseThrow(() -> new NoSuchElementException("Reply not found: " + replyId));

        reply.setMediaUrls(new ArrayList<>());
        reply.setMediaApproved(true);
        reply.setMediaPendingReview(false);
        forumReplyRepository.save(reply);
    }

    @Transactional
    public void approveReportedReply(Long postId, Long replyId, Long adminUserId) {
        ensureAdmin(adminUserId);
        ForumReply reply = forumReplyRepository.findByIdAndPostId(replyId, postId)
                .orElseThrow(() -> new NoSuchElementException("Reply not found: " + replyId));
        reply.setHiddenByReports(false);
        reply.setHiddenAt(null);
        reply.setReportCount(0);
        forumReplyRepository.save(reply);
        markReportsReviewed("REPLY", replyId, adminUserId, "APPROVED");
    }

    @Transactional
    public void rejectReportedReply(Long postId, Long replyId, Long adminUserId) {
        ensureAdmin(adminUserId);
        deleteReply(postId, replyId, adminUserId);
        markReportsReviewed("REPLY", replyId, adminUserId, "REJECTED");
    }

    @Transactional
    public void approveReportedComment(Long postId, Long replyId, Long commentId, Long adminUserId) {
        ensureAdmin(adminUserId);
        ForumReply reply = forumReplyRepository.findByIdAndPostId(replyId, postId)
                .orElseThrow(() -> new NoSuchElementException("Reply not found: " + replyId));
        ForumComment comment = reply.getComments().stream()
                .filter(item -> item.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Comment not found: " + commentId));
        comment.setHiddenByReports(false);
        comment.setHiddenAt(null);
        comment.setReportCount(0);
        forumCommentRepository.save(comment);
        markReportsReviewed("COMMENT", commentId, adminUserId, "APPROVED");
    }

    @Transactional
    public void rejectReportedComment(Long postId, Long replyId, Long commentId, Long adminUserId) {
        ensureAdmin(adminUserId);
        deleteComment(postId, replyId, commentId, adminUserId);
        markReportsReviewed("COMMENT", commentId, adminUserId, "REJECTED");
    }

    @Transactional(readOnly = true)
    public ForumUser getUserById(Long userId) {
        if (AI_ASSISTANT_USER_ID.equals(userId)) {
            ForumUser aiUser = new ForumUser();
            aiUser.setId(AI_ASSISTANT_USER_ID);
            aiUser.setName("GreenRoots AI");
            aiUser.setReputation(9999);
            aiUser.setBadgeTier("AI_ASSISTANT");
            aiUser.setBadgeLabel("AI Assistant");
            aiUser.setExpert(true);
            return aiUser;
        }

        UserServiceClient.UserSummary summary = userServiceClient.getUserSummary(userId);
        ForumUser user = new ForumUser();
        user.setId(summary.id());
        user.setName(summary.name());
        int reputation = reputationService.calculateReputation(userId);
        user.setReputation(reputation);
        BadgeTier badgeTier = reputationService.calculateBadgeTier(reputation);
        user.setBadgeTier(badgeTier.name());
        user.setBadgeLabel(badgeTier.getLabel());
        user.setExpert(summary.isExpert());
        return user;
    }

    private void saveReport(String targetType, Long targetId, Long postId, Long replyId, Long commentId, Long reporterId, CreateReportRequest request) {
        ForumReport report = new ForumReport();
        report.setTargetType(targetType.toUpperCase());
        report.setTargetId(targetId);
        report.setPostId(postId);
        report.setReplyId(replyId);
        report.setCommentId(commentId);
        report.setReporterId(reporterId);
        report.setReason(request.reason().trim());
        report.setScreenshotDataUrl(normalizeScreenshotDataUrl(request.screenshotDataUrl()));
        report.setCreatedAt(nowIso());
        report.setStatus("PENDING");
        forumReportRepository.save(report);
    }

    private void markReportsReviewed(String targetType, Long targetId, Long adminUserId, String status) {
        List<ForumReport> relatedReports = forumReportRepository.findByTargetTypeAndTargetIdOrderByIdDesc(targetType.toUpperCase(), targetId);
        if (relatedReports.isEmpty()) {
            return;
        }

        String reviewedAt = nowIso();
        relatedReports.forEach(report -> {
            if (!"PENDING".equals(report.getStatus())) {
                return;
            }

            report.setStatus(status);
            report.setReviewedBy(adminUserId);
            report.setReviewedAt(reviewedAt);
            forumReportRepository.save(report);
        });
    }

    private String normalizeScreenshotDataUrl(String screenshotDataUrl) {
        if (screenshotDataUrl == null) {
            return null;
        }

        String trimmed = screenshotDataUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> sanitizeTags(List<String> tags) {
        if (tags == null) {
            return List.of("General");
        }

        List<String> cleaned = tags.stream()
                .map(tag -> tag == null ? "" : tag.trim())
                .filter(tag -> !tag.isBlank())
                .map(tag -> tag.substring(0, Math.min(tag.length(), 40)))
                .distinct()
                .limit(5)
                .toList();

        if (cleaned.isEmpty()) {
            return List.of("General");
        }

        return cleaned;
    }

    private String sanitizeRichText(String input) {
        if (input == null) {
            return "";
        }

        String sanitized = SCRIPT_TAG_PATTERN.matcher(input).replaceAll("");
        sanitized = INLINE_EVENT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = JS_URI_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = sanitized.trim();

        if (sanitized.length() > 5500) {
            sanitized = sanitized.substring(0, 5500).trim();
        }
        return sanitized;
    }

    private List<String> sanitizeMediaUrls(List<String> mediaUrls) {
        if (mediaUrls == null) {
            return List.of();
        }

        return mediaUrls.stream()
                .map(url -> url == null ? "" : url.trim())
                .filter(url -> !url.isBlank())
                .filter(url -> url.startsWith("data:") || url.startsWith("http://") || url.startsWith("https://"))
                .map(url -> normalizeMediaUrl(url))
                .distinct()
                .limit(6)
                .toList();
    }

    private String normalizeMediaUrl(String url) {
        if (url.startsWith("data:")) {
            return url;
        }

        return url.substring(0, Math.min(url.length(), 4096));
    }

    private String nowIso() {
        return OffsetDateTime.now().toString();
    }

    private String resolveGroupName(Long groupId) {
        if (groupId == null) {
            return null;
        }

        return forumGroupRepository.findById(groupId)
                .map(ForumGroup::getName)
                .orElse(null);
    }

    private List<String> resolveGroupRules(Long groupId) {
        if (groupId == null) {
            return List.of();
        }

        return forumGroupRepository.findById(groupId)
                .map(group -> group.getRules() == null ? List.<String>of() : group.getRules())
                .orElse(List.of());
    }

    public Long resolveAuthenticatedUserId(String authorizationHeader) {
        return userServiceClient.validateAndGetUserId(authorizationHeader);
    }

    private void ensureUserExists(Long userId) {
        userServiceClient.ensureUserExists(userId);
    }

    private void ensureOwnerOrAdmin(Long userId, Long authorId) {
        if (authorId != null && authorId.equals(userId)) {
            return;
        }

        if (!isAdmin(userId)) {
            throw new IllegalArgumentException("You are not allowed to delete this content");
        }
    }

    private boolean isAdmin(Long userId) {
        UserServiceClient.UserSummary summary = userServiceClient.getUserSummary(userId);
        return summary.role() != null && summary.role().toUpperCase().contains("ADMIN");
    }

    private boolean isAdminView(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            return isAdmin(userId);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void ensureAdmin(Long userId) {
        ensureUserExists(userId);
        if (!isAdmin(userId)) {
            throw new IllegalArgumentException("Only admins can perform moderation review");
        }
    }

    private void ensureTargetNotAlreadyReportedByUser(String targetType, Long targetId, Long reporterId) {
        if (forumReportRepository.existsByTargetTypeAndTargetIdAndReporterIdAndStatus(
                targetType.toUpperCase(),
                targetId,
                reporterId,
                REPORT_STATUS_PENDING
        )) {
            throw new IllegalArgumentException("You already reported this content and it is still under review");
        }
    }

    private void hydrateReportState(List<ForumPost> posts, Long currentUserId) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        List<Long> postIds = posts.stream().map(ForumPost::getId).toList();
        List<ForumReport> pendingReports = forumReportRepository.findByPostIdInAndStatusOrderByIdDesc(postIds, REPORT_STATUS_PENDING);

        Map<Long, Integer> activeReportCountsByPostId = new HashMap<>();
        Map<String, Boolean> currentUserPendingByTarget = new HashMap<>();

        for (ForumReport report : pendingReports) {
            activeReportCountsByPostId.merge(report.getPostId(), 1, Integer::sum);
            if (currentUserId != null && currentUserId.equals(report.getReporterId())) {
                currentUserPendingByTarget.put(reportKey(report.getTargetType(), report.getTargetId()), Boolean.TRUE);
            }
        }

        for (ForumPost post : posts) {
            post.setActiveReportCount(activeReportCountsByPostId.getOrDefault(post.getId(), 0));
            post.setCurrentUserHasPendingReport(Boolean.TRUE.equals(currentUserPendingByTarget.get(reportKey("POST", post.getId()))));

            for (ForumReply reply : post.getReplies()) {
                reply.setCurrentUserHasPendingReport(Boolean.TRUE.equals(currentUserPendingByTarget.get(reportKey("REPLY", reply.getId()))));

                for (ForumComment comment : reply.getComments()) {
                    comment.setCurrentUserHasPendingReport(Boolean.TRUE.equals(currentUserPendingByTarget.get(reportKey("COMMENT", comment.getId()))));
                }
            }
        }
    }

    private String reportKey(String targetType, Long targetId) {
        return targetType.toUpperCase() + ":" + targetId;
    }

    private void applyCurrentUserVotes(List<ForumPost> posts, Long currentUserId) {
        if (currentUserId == null) {
            return;
        }

        for (ForumPost post : posts) {
            Map<Long, String> votesByReplyId = new HashMap<>();
            try {
                forumReplyVoteRepository.findAllByPostIdAndUserId(post.getId(), currentUserId)
                        .forEach(vote -> {
                            if (vote.getReply() != null && vote.getReply().getId() != null) {
                                votesByReplyId.put(vote.getReply().getId(), vote.getVoteType());
                            }
                        });
            } catch (RuntimeException ignored) {
                // Do not fail the whole feed if user-specific vote hydration fails.
            }

            post.getReplies().forEach(reply -> reply.setCurrentUserVote(votesByReplyId.get(reply.getId())));
        }
    }

    private void enforceDeletedContentPolicy(ForumPost post) {
        if (post == null) {
            return;
        }

        if (post.isDeleted()) {
            post.setTitle("Post was removed");
            post.setContent(post.isDeletedByAdmin()
                    ? "This post was removed by an administrator."
                    : "This post was removed by the user.");
        }

        for (ForumReply reply : post.getReplies()) {
            if (reply.isDeleted()) {
                reply.setContent(reply.isDeletedByAdmin()
                        ? "This reply has been removed by an administrator."
                        : "This reply has been removed by the user.");
                reply.setUpvotes(0);
                reply.setDownvotes(0);
                reply.getComments().clear();
                continue;
            }

            for (ForumComment comment : reply.getComments()) {
                if (comment.isDeleted()) {
                    comment.setContent(comment.isDeletedByAdmin()
                            ? "This comment has been removed by an administrator."
                            : "This comment has been removed by the user.");
                }
            }
        }
    }

    private void enforceModerationVisibility(ForumPost post, boolean adminView) {
        if (post == null) {
            return;
        }

        boolean mediaPendingReview = post.getMediaUrls() != null
                && !post.getMediaUrls().isEmpty()
                && !post.isMediaApproved();
        post.setMediaPendingReview(mediaPendingReview);

        if (!adminView && mediaPendingReview) {
            post.setMediaUrls(List.of());
        }

        for (ForumReply reply : post.getReplies()) {
            boolean replyMediaPendingReview = reply.getMediaUrls() != null
                    && !reply.getMediaUrls().isEmpty()
                    && !reply.isMediaApproved();
            reply.setMediaPendingReview(replyMediaPendingReview);

            if (!adminView && replyMediaPendingReview) {
                reply.setMediaUrls(List.of());
            }
        }
    }

    private void initializeMediaCollections(ForumPost post) {
        if (post == null) {
            return;
        }

        if (post.getTags() != null) {
            post.getTags().size();
        }

        if (post.getMediaUrls() != null) {
            post.getMediaUrls().size();
        }

        if (post.getReplies() == null) {
            return;
        }

        for (ForumReply reply : post.getReplies()) {
            if (reply.getMediaUrls() != null) {
                reply.getMediaUrls().size();
            }
        }
    }

    private void applyBestAnswerRule(ForumPost post) {
        if (post == null || post.getReplies().isEmpty()) {
            return;
        }

        ForumReply bestReply = null;
        int bestScore = Integer.MIN_VALUE;

        for (ForumReply reply : post.getReplies()) {
            if (reply.isDeleted()) {
                reply.setAccepted(false);
                continue;
            }
            int score = reply.getUpvotes() - reply.getDownvotes();
            if (score > bestScore) {
                bestScore = score;
                bestReply = reply;
            } else if (score == bestScore && bestReply != null) {
                String currentCreatedAt = reply.getCreatedAt();
                String bestCreatedAt = bestReply.getCreatedAt();
                if (bestCreatedAt == null || (currentCreatedAt != null && currentCreatedAt.compareTo(bestCreatedAt) < 0)) {
                    bestReply = reply;
                }
            }
        }

        for (ForumReply reply : post.getReplies()) {
            reply.setAccepted(bestReply != null && reply.getId().equals(bestReply.getId()));
        }

        forumPostRepository.save(post);
    }

    public void markReplyAsAccepted(Long postId, Long replyId, Long userId) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + postId));

        if (!post.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("Only the post author can mark replies as accepted");
        }

        ForumReply reply = post.getReplies().stream()
                .filter(r -> r.getId().equals(replyId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Reply not found: " + replyId));

        // Toggle the accepted status
        reply.setAccepted(!reply.isAccepted());
        forumPostRepository.save(post);
    }

    public Map<String, Object> getUserProfile(Long userId) {
        UserServiceClient.UserSummary userSummary = userServiceClient.getUserSummary(userId);
        
        long postCount = forumPostRepository.countByAuthorId(userId);
        long replyCount = forumReplyRepository.countByAuthorId(userId);
        long acceptedReplyCount = forumReplyRepository.countByAuthorIdAndIsAcceptedTrue(userId);
        
        int reputation = reputationService.calculateReputation(userId);
        String badgeTier = reputationService.calculateBadgeTier(reputation).getLabel();

        return Map.ofEntries(
            Map.entry("userId", userId),
            Map.entry("name", userSummary.name()),
            Map.entry("reputation", reputation),
            Map.entry("badgeTier", badgeTier),
            Map.entry("isExpert", userSummary.isExpert()),
            Map.entry("role", userSummary.role()),
            Map.entry("postCount", postCount),
            Map.entry("replyCount", replyCount),
            Map.entry("acceptedReplyCount", acceptedReplyCount)
        );
    }

    @Transactional(readOnly = true)
    public List<ForumGroup> getGroups(Long currentUserId) {
        List<ForumGroup> groups = forumGroupRepository.findAllByOrderByIdDesc();
        groups.forEach(group -> hydrateGroupState(group, currentUserId));
        return groups;
    }

    @Transactional(readOnly = true)
    public ForumGroup getGroupById(Long groupId, Long currentUserId) {
        ForumGroup group = forumGroupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));
        hydrateGroupState(group, currentUserId);
        return group;
    }

    @Transactional
    public ForumGroup createGroup(CreateGroupRequest request, Long creatorUserId) {
        ensureUserExists(creatorUserId);

        String normalizedName = request.name().trim();
        forumGroupRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            throw new IllegalArgumentException("Group name already exists");
        });

        ForumGroup group = new ForumGroup();
        group.setName(normalizedName);
        group.setDescription(request.description().trim());
        group.setCreatedAt(nowIso());
        group.setCreatedBy(creatorUserId);
        group.setFocusTags(sanitizeTags(request.focusTags()));
        group.setRules(sanitizeRules(request.rules()));
        group.setMemberIds(new ArrayList<>(List.of(creatorUserId)));
        group.setModeratorIds(new ArrayList<>(List.of(creatorUserId)));

        ForumGroup saved = forumGroupRepository.save(group);
        hydrateGroupState(saved, creatorUserId);
        return saved;
    }

    @Transactional
    public void joinGroup(Long groupId, Long userId) {
        ensureUserExists(userId);
        ForumGroup group = forumGroupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));

        List<Long> memberIds = new ArrayList<>(group.getMemberIds());
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
            group.setMemberIds(memberIds);
            forumGroupRepository.save(group);
        }
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        ensureUserExists(userId);
        ForumGroup group = forumGroupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));

        List<Long> memberIds = new ArrayList<>(group.getMemberIds());
        if (memberIds.remove(userId)) {
            group.setMemberIds(memberIds);
        }

        List<Long> moderatorIds = new ArrayList<>(group.getModeratorIds());
        if (moderatorIds.remove(userId)) {
            if (moderatorIds.isEmpty() && !memberIds.isEmpty()) {
                moderatorIds.add(memberIds.get(0));
            }
            group.setModeratorIds(moderatorIds);
        }

        forumGroupRepository.save(group);
    }

    private List<ForumPost> applySearchAndTagFilters(List<ForumPost> posts, String searchTerm, List<String> tags) {
        List<ForumPost> filtered = new ArrayList<>(posts);

        if (searchTerm != null && !searchTerm.trim().isBlank()) {
            String query = searchTerm.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(post ->
                            (post.getTitle() != null && post.getTitle().toLowerCase().contains(query))
                                    || (post.getContent() != null && post.getContent().toLowerCase().contains(query)))
                    .toList();
        }

        if (tags != null && !tags.isEmpty()) {
            List<String> normalizedTags = tags.stream()
                    .map(tag -> tag == null ? "" : tag.trim().toLowerCase())
                    .filter(tag -> !tag.isBlank())
                    .toList();

            if (!normalizedTags.isEmpty()) {
                filtered = filtered.stream()
                        .filter(post -> post.getTags().stream().anyMatch(tag -> normalizedTags.contains(tag.toLowerCase())))
                        .toList();
            }
        }

        return filtered;
    }

    private List<String> sanitizeRules(List<String> rules) {
        List<String> defaultRules = List.of(
                "Be respectful and practical.",
                "Use clear titles and field context.",
                "No spam, harassment, or unsafe advice.");

        if (rules == null || rules.isEmpty()) {
            return defaultRules;
        }

        List<String> cleaned = rules.stream()
                .map(rule -> rule == null ? "" : rule.trim())
                .filter(rule -> !rule.isBlank())
                .map(rule -> rule.substring(0, Math.min(rule.length(), 300)))
                .distinct()
                .limit(8)
                .toList();

        return cleaned.isEmpty() ? defaultRules : cleaned;
    }

    private void hydrateGroupState(ForumGroup group, Long currentUserId) {
        if (group == null) {
            return;
        }

        List<Long> memberIds = group.getMemberIds() == null ? List.of() : group.getMemberIds();
        group.setMemberCount(memberIds.size());
        group.setJoined(currentUserId != null && memberIds.contains(currentUserId));
    }

    private List<ForumPost> applySorting(List<ForumPost> posts, String sortBy, String sortDirection, boolean adminView) {
        boolean ascending = "asc".equalsIgnoreCase(sortDirection);
        
        return switch (sortBy.toLowerCase()) {
            case "reported" -> {
                if (!adminView) {
                    yield posts;
                }

                List<ForumPost> reportedPosts = posts.stream()
                        .filter(post -> post.getActiveReportCount() > 0 || post.isHiddenByReports())
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

                reportedPosts.sort((a, b) -> {
                    int reportComparison = Integer.compare(b.getActiveReportCount(), a.getActiveReportCount());
                    if (reportComparison != 0) {
                        return reportComparison;
                    }
                    return Long.compare(b.getId(), a.getId());
                });

                yield reportedPosts;
            }
            case "votes", "popular" -> {
                posts.sort((a, b) -> {
                    int aVotes = a.getReplies().stream().mapToInt(r -> r.getUpvotes() - r.getDownvotes()).sum();
                    int bVotes = b.getReplies().stream().mapToInt(r -> r.getUpvotes() - r.getDownvotes()).sum();
                    return ascending ? Integer.compare(aVotes, bVotes) : Integer.compare(bVotes, aVotes);
                });
                yield posts;
            }
            case "commented", "discussed" -> {
                posts.sort((a, b) -> {
                    int aCom = a.getReplies().size();
                    int bCom = b.getReplies().size();
                    return ascending ? Integer.compare(aCom, bCom) : Integer.compare(bCom, aCom);
                });
                yield posts;
            }
            default -> posts; // "newest" or default - already sorted by ID desc
        };
    }
}
