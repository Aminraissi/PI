package tn.esprit.forums.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "forum_comments")
public class ForumComment {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @JsonIgnore
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "reply_id", nullable = false)
        private ForumReply reply;

        @Column(nullable = false, name = "author_id")
        private Long authorId;

        @Column(nullable = false, length = 2500)
        private String content;

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
        private boolean currentUserHasPendingReport;

        public ForumComment() {
        }

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public ForumReply getReply() {
                return reply;
        }

        public void setReply(ForumReply reply) {
                this.reply = reply;
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

        @JsonProperty("currentUserHasPendingReport")
        public boolean isCurrentUserHasPendingReport() {
                return currentUserHasPendingReport;
        }

        public void setCurrentUserHasPendingReport(boolean currentUserHasPendingReport) {
                this.currentUserHasPendingReport = currentUserHasPendingReport;
        }
}
