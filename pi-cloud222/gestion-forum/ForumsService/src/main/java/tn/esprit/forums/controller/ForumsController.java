package tn.esprit.forums.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.forums.dto.CreateCommentRequest;
import tn.esprit.forums.dto.AiDuplicateCandidateResponse;
import tn.esprit.forums.dto.AiDuplicateDetectionRequest;
import tn.esprit.forums.dto.AiModerationAnalysisRequest;
import tn.esprit.forums.dto.AiModerationAnalysisResponse;
import tn.esprit.forums.dto.AiReplyImproveRequest;
import tn.esprit.forums.dto.CreateGroupRequest;
import tn.esprit.forums.dto.CreatePostRequest;
import tn.esprit.forums.dto.CreateReplyRequest;
import tn.esprit.forums.dto.CreateReportRequest;
import tn.esprit.forums.dto.AiTagSuggestionRequest;
import tn.esprit.forums.model.ForumGroup;
import tn.esprit.forums.model.ForumPost;
import tn.esprit.forums.model.ForumReport;
import tn.esprit.forums.model.ForumUser;
import tn.esprit.forums.service.ForumAiSuggestionService;
import tn.esprit.forums.service.ForumsService;

@RestController
@RequestMapping("/api/forums")
public class ForumsController {

    private final ForumsService forumsService;
    private final ForumAiSuggestionService forumAiSuggestionService;

    public ForumsController(ForumsService forumsService, ForumAiSuggestionService forumAiSuggestionService) {
        this.forumsService = forumsService;
        this.forumAiSuggestionService = forumAiSuggestionService;
    }

    @GetMapping("/posts")
    public List<ForumPost> getPosts(
            @RequestParam(name = "userId", required = false) Long userId,
            @RequestParam(name = "sortBy", defaultValue = "newest") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "tags", required = false) String tags,
            @RequestParam(name = "groupId", required = false) Long groupId
    ) {
        List<String> parsedTags = tags == null || tags.isBlank()
                ? List.of()
                : java.util.Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
        return forumsService.getPosts(userId, sortBy, sortDirection, search, parsedTags, groupId);
    }

    @GetMapping("/posts/{postId}")
    public ForumPost getPostById(@PathVariable Long postId, @RequestParam(name = "userId", required = false) Long userId) {
        return forumsService.getPostById(postId, userId);
    }

    @PostMapping("/posts")
    public ResponseEntity<ForumPost> createPost(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CreatePostRequest request
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        CreatePostRequest securedRequest = new CreatePostRequest(
                request.title(),
                request.content(),
                request.tags(),
            request.mediaUrls(),
            request.groupId(),
            userId,
            request.generateAiReply()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(forumsService.createPost(securedRequest));
    }

    @GetMapping("/groups")
    public List<ForumGroup> getGroups(@RequestParam(name = "userId", required = false) Long userId) {
        return forumsService.getGroups(userId);
    }

    @GetMapping("/groups/{groupId}")
    public ForumGroup getGroupById(
            @PathVariable Long groupId,
            @RequestParam(name = "userId", required = false) Long userId
    ) {
        return forumsService.getGroupById(groupId, userId);
    }

    @PostMapping("/groups")
    public ResponseEntity<ForumGroup> createGroup(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(forumsService.createGroup(request, userId));
    }

    @PostMapping("/groups/{groupId}/join")
    public ResponseEntity<Void> joinGroup(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long groupId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.joinGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/groups/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long groupId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.leaveGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/replies")
    public ResponseEntity<Void> addReply(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @Valid @RequestBody CreateReplyRequest request
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        CreateReplyRequest securedRequest = new CreateReplyRequest(request.content(), request.mediaUrls(), userId);
        forumsService.addReply(postId, securedRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/vote")
    public ResponseEntity<Void> voteReply(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @PathVariable Long replyId,
            @RequestParam("type") String voteType
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.voteReply(postId, replyId, voteType, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/comments")
    public ResponseEntity<Void> addComment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @PathVariable Long replyId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        CreateCommentRequest securedRequest = new CreateCommentRequest(request.content(), userId);
        forumsService.addComment(postId, replyId, securedRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/delete")
    public ResponseEntity<Void> deleteReply(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @PathVariable Long replyId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.deleteReply(postId, replyId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/comments/{commentId}/delete")
    public ResponseEntity<Void> deleteComment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @PathVariable Long replyId,
            @PathVariable Long commentId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.deleteComment(postId, replyId, commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/delete")
    public ResponseEntity<Void> deletePost(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/report")
    public ResponseEntity<Map<String, Object>> reportPost(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        return ResponseEntity.ok(forumsService.reportPost(postId, userId, request));
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/report")
    public ResponseEntity<Map<String, Object>> reportReply(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @PathVariable Long replyId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        return ResponseEntity.ok(forumsService.reportReply(postId, replyId, userId, request));
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/comments/{commentId}/report")
    public ResponseEntity<Map<String, Object>> reportComment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @PathVariable Long replyId,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        return ResponseEntity.ok(forumsService.reportComment(postId, replyId, commentId, userId, request));
    }

    @GetMapping("/reports/{targetType}/{targetId}")
    public List<ForumReport> getReportsForTarget(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String targetType,
            @PathVariable Long targetId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        return forumsService.getReportsForTarget(targetType, targetId, userId);
    }

    @PostMapping("/posts/{postId}/moderation/approve")
    public ResponseEntity<Void> approveReportedPost(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.approveReportedPost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/moderation/reject")
    public ResponseEntity<Void> rejectReportedPost(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.rejectReportedPost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/media/approve")
    public ResponseEntity<Void> approvePostMedia(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.approvePostMedia(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/media/reject")
    public ResponseEntity<Void> rejectPostMedia(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.rejectPostMedia(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/moderation/approve")
    public ResponseEntity<Void> approveReportedReply(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @PathVariable Long replyId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.approveReportedReply(postId, replyId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/moderation/reject")
    public ResponseEntity<Void> rejectReportedReply(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @PathVariable Long replyId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.rejectReportedReply(postId, replyId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/comments/{commentId}/moderation/approve")
    public ResponseEntity<Void> approveReportedComment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @PathVariable Long replyId,
            @PathVariable Long commentId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.approveReportedComment(postId, replyId, commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/comments/{commentId}/moderation/reject")
    public ResponseEntity<Void> rejectReportedComment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @PathVariable Long replyId,
            @PathVariable Long commentId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.rejectReportedComment(postId, replyId, commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}")
    public ForumUser getUser(@PathVariable Long userId) {
        return forumsService.getUserById(userId);
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(forumsService.getUserProfile(userId));
    }

    @PostMapping("/posts/{postId}/replies/{replyId}/accept")
    public ResponseEntity<Void> acceptReply(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long postId,
            @PathVariable Long replyId
    ) {
        Long userId = resolveAuthenticatedUserId(authorization);
        forumsService.markReplyAsAccepted(postId, replyId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "forums-service");
    }

    @GetMapping("/health/ai")
    public Map<String, Object> aiHealth() {
        return forumAiSuggestionService.getRuntimeStatus();
    }

    @PostMapping("/ai/tags")
    public Map<String, List<String>> suggestTags(@RequestBody AiTagSuggestionRequest request) {
        List<String> suggestions = forumsService.suggestTags(
                request.title(),
                request.content(),
                request.tags(),
                request.groupId()
        );
        return Map.of("tags", suggestions);
    }

    @PostMapping("/ai/duplicates")
    public List<AiDuplicateCandidateResponse> suggestDuplicates(@RequestBody AiDuplicateDetectionRequest request) {
        return forumsService.findDuplicateCandidates(
                request.title(),
                request.content(),
                request.tags(),
                request.groupId()
        );
    }

    @PostMapping("/ai/replies/improve")
    public Map<String, String> improveReplyDraft(@RequestBody AiReplyImproveRequest request) {
        String improved = forumsService.improveReplyDraft(
                request.draft(),
                request.postTitle(),
                request.postContent(),
                request.groupId()
        );
        return Map.of("draft", improved);
    }

    @PostMapping("/ai/moderation/analyze")
    public AiModerationAnalysisResponse analyzeModerationCase(@RequestBody AiModerationAnalysisRequest request) {
        return forumsService.analyzeModerationCase(request)
                .orElseGet(() -> new AiModerationAnalysisResponse(
                        "REVIEW_CAREFULLY",
                        "MEDIUM",
                        "The moderation assistant could not complete a direct content analysis right now.",
                        List.of("Fallback moderation review")
                ));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    private Long resolveAuthenticatedUserId(String authorizationHeader) {
        return forumsService.resolveAuthenticatedUserId(authorizationHeader);
    }
}
