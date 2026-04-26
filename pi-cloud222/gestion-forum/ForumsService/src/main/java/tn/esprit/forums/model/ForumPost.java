package tn.esprit.forums.model;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forum_posts")
public class ForumPost {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, length = 200)
        private String title;

        @Column(nullable = false, length = 6000)
        private String content;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "forum_post_tags", joinColumns = @JoinColumn(name = "post_id"))
        @Column(name = "tag", nullable = false, length = 40)
        private List<String> tags = new ArrayList<>();

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "forum_post_media", joinColumns = @JoinColumn(name = "post_id"))
        @Column(name = "media_url", nullable = false, columnDefinition = "LONGTEXT")
        private List<String> mediaUrls = new ArrayList<>();

        @Column(nullable = false)
        private boolean mediaApproved = true;

        @Column(nullable = false, name = "author_id")
        private Long authorId;

        @Column(name = "group_id")
        private Long groupId;

        @Column(nullable = false, length = 50)
        private String createdAt;

        @Column(nullable = false)
        private int views;

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
        private int activeReportCount;

        @Transient
        private boolean currentUserHasPendingReport;

        @Transient
        private boolean mediaPendingReview;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderBy("id ASC")
        private List<ForumReply> replies = new ArrayList<>();

        public ForumPost() {
        }

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public String getTitle() {
                return title;
        }

        public void setTitle(String title) {
                this.title = title;
        }

        public String getContent() {
                return content;
        }

        public void setContent(String content) {
                this.content = content;
        }

        public List<String> getTags() {
                return tags;
        }

        public void setTags(List<String> tags) {
                this.tags = tags;
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

        public Long getAuthorId() {
                return authorId;
        }

        public void setAuthorId(Long authorId) {
                this.authorId = authorId;
        }

        public Long getGroupId() {
                return groupId;
        }

        public void setGroupId(Long groupId) {
                this.groupId = groupId;
        }

        public String getCreatedAt() {
                return createdAt;
        }

        public void setCreatedAt(String createdAt) {
                this.createdAt = createdAt;
        }

        public int getViews() {
                return views;
        }

        public void setViews(int views) {
                this.views = views;
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

        @JsonProperty("activeReportCount")
        public int getActiveReportCount() {
                return activeReportCount;
        }

        public void setActiveReportCount(int activeReportCount) {
                this.activeReportCount = activeReportCount;
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

        public List<ForumReply> getReplies() {
                return replies;
        }

        public void setReplies(List<ForumReply> replies) {
                this.replies = replies;
        }

        public void addReply(ForumReply reply) {
                replies.add(reply);
                reply.setPost(this);
        }
}
