package tn.esprit.forums.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "forum_reports")
public class ForumReport {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, length = 20)
        private String targetType;

        @Column(nullable = false)
        private Long targetId;

        @Column(name = "post_id")
        private Long postId;

        @Column(name = "reply_id")
        private Long replyId;

        @Column(name = "comment_id")
        private Long commentId;

        @Column(nullable = false, name = "reporter_id")
        private Long reporterId;

        @Column(nullable = false, length = 2000)
        private String reason;

        @Column(length = 12000)
        private String screenshotDataUrl;

        @Column(nullable = false, length = 50)
        private String createdAt;

        @Column(nullable = false, length = 20)
        private String status;

        @Column(length = 50)
        private String reviewedAt;

        @Column(name = "reviewed_by")
        private Long reviewedBy;

        @Column(length = 200)
        private String adminNotes;

        public ForumReport() {
        }

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public String getTargetType() {
                return targetType;
        }

        public void setTargetType(String targetType) {
                this.targetType = targetType;
        }

        public Long getTargetId() {
                return targetId;
        }

        public void setTargetId(Long targetId) {
                this.targetId = targetId;
        }

        public Long getPostId() {
                return postId;
        }

        public void setPostId(Long postId) {
                this.postId = postId;
        }

        public Long getReplyId() {
                return replyId;
        }

        public void setReplyId(Long replyId) {
                this.replyId = replyId;
        }

        public Long getCommentId() {
                return commentId;
        }

        public void setCommentId(Long commentId) {
                this.commentId = commentId;
        }

        public Long getReporterId() {
                return reporterId;
        }

        public void setReporterId(Long reporterId) {
                this.reporterId = reporterId;
        }

        public String getReason() {
                return reason;
        }

        public void setReason(String reason) {
                this.reason = reason;
        }

        public String getScreenshotDataUrl() {
                return screenshotDataUrl;
        }

        public void setScreenshotDataUrl(String screenshotDataUrl) {
                this.screenshotDataUrl = screenshotDataUrl;
        }

        public String getCreatedAt() {
                return createdAt;
        }

        public void setCreatedAt(String createdAt) {
                this.createdAt = createdAt;
        }

        public String getStatus() {
                return status;
        }

        public void setStatus(String status) {
                this.status = status;
        }

        public String getReviewedAt() {
                return reviewedAt;
        }

        public void setReviewedAt(String reviewedAt) {
                this.reviewedAt = reviewedAt;
        }

        public Long getReviewedBy() {
                return reviewedBy;
        }

        public void setReviewedBy(Long reviewedBy) {
                this.reviewedBy = reviewedBy;
        }

        public String getAdminNotes() {
                return adminNotes;
        }

        public void setAdminNotes(String adminNotes) {
                this.adminNotes = adminNotes;
        }
}
