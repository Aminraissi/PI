package tn.esprit.forums.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forum_replies")
public class ForumReply {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @JsonIgnore
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_id", nullable = false)
        private ForumPost post;

        @Column(nullable = false, name = "author_id")
        private Long authorId;

        @Column(nullable = false, length = 6000)
        private String content;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "forum_reply_media", joinColumns = @JoinColumn(name = "reply_id"))
        @Column(name = "media_url", nullable = false, columnDefinition = "LONGTEXT")
        private List<String> mediaUrls = new ArrayList<>();

        @Column(nullable = false)
        private boolean mediaApproved = true;

        @Column(nullable = false)
        private int upvotes;

        @Column(nullable = false)
        private int downvotes;

        @Column(nullable = false)
        private boolean isAccepted;

        @Column(nullable = false, length = 50)
        private String createdAt;

        @Column(nullable = false)
        private boolean deleted;

        @Column(nullable = false)
        private boolean deletedByAdmin;

        @Column(length = 50)
        private String deletedAt;

        @Column(nullable = false)
        private int reportCount;

        @Column(nullable = false)
        private boolean hiddenByReports;

        @Column(length = 50)
        private String hiddenAt;

        @Transient
        private String currentUserVote;

        @Transient
        private boolean currentUserHasPendingReport;

        @Transient
        private boolean mediaPendingReview;

        @OneToMany(mappedBy = "reply", cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderBy("id ASC")
        private List<ForumComment> comments = new ArrayList<>();

        public ForumReply() {
        }

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public ForumPost getPost() {
                return post;
        }

        public void setPost(ForumPost post) {
                this.post = post;
        }

        public Long getAuthorId() {
                return authorId;
        }

        public void setAuthorId(Long authorId) {
                this.authorId = authorId;
        }

        public String getContent() {
                return content;
        }

        public void setContent(String content) {
                this.content = content;
        }

        public List<String> getMediaUrls() {
                return mediaUrls;
        }

        public void setMediaUrls(List<String> mediaUrls) {
                this.mediaUrls = mediaUrls;
        }

        public boolean isMediaApproved() {
                return mediaApproved;
        }

        public void setMediaApproved(boolean mediaApproved) {
                this.mediaApproved = mediaApproved;
        }

        public int getUpvotes() {
                return upvotes;
        }

        public void setUpvotes(int upvotes) {
                this.upvotes = upvotes;
        }

        public int getDownvotes() {
                return downvotes;
        }

        public void setDownvotes(int downvotes) {
                this.downvotes = downvotes;
        }

        @JsonProperty("isAccepted")
        public boolean isAccepted() {
                return isAccepted;
        }

        public void setAccepted(boolean accepted) {
                isAccepted = accepted;
        }

        public String getCreatedAt() {
                return createdAt;
        }

        public void setCreatedAt(String createdAt) {
                this.createdAt = createdAt;
        }

        @JsonProperty("isDeleted")
        public boolean isDeleted() {
                return deleted;
        }

        public void setDeleted(boolean deleted) {
                this.deleted = deleted;
        }

        public boolean isDeletedByAdmin() {
                return deletedByAdmin;
        }

        public void setDeletedByAdmin(boolean deletedByAdmin) {
                this.deletedByAdmin = deletedByAdmin;
        }

        public String getDeletedAt() {
                return deletedAt;
        }

        public void setDeletedAt(String deletedAt) {
                this.deletedAt = deletedAt;
        }

        public int getReportCount() {
                return reportCount;
        }

        public void setReportCount(int reportCount) {
                this.reportCount = reportCount;
        }

        @JsonProperty("isHiddenByReports")
        public boolean isHiddenByReports() {
                return hiddenByReports;
        }

        public void setHiddenByReports(boolean hiddenByReports) {
                this.hiddenByReports = hiddenByReports;
        }

        public String getHiddenAt() {
                return hiddenAt;
        }

        public void setHiddenAt(String hiddenAt) {
                this.hiddenAt = hiddenAt;
        }

        public String getCurrentUserVote() {
                return currentUserVote;
        }

        public void setCurrentUserVote(String currentUserVote) {
                this.currentUserVote = currentUserVote;
        }

        @JsonProperty("currentUserHasPendingReport")
        public boolean isCurrentUserHasPendingReport() {
                return currentUserHasPendingReport;
        }

        public void setCurrentUserHasPendingReport(boolean currentUserHasPendingReport) {
                this.currentUserHasPendingReport = currentUserHasPendingReport;
        }

        public boolean isMediaPendingReview() {
                return mediaPendingReview;
        }

        public void setMediaPendingReview(boolean mediaPendingReview) {
                this.mediaPendingReview = mediaPendingReview;
        }

        public List<ForumComment> getComments() {
                return comments;
        }

        public void setComments(List<ForumComment> comments) {
                this.comments = comments;
        }

        public void addComment(ForumComment comment) {
                comments.add(comment);
                comment.setReply(this);
        }
}
