package tn.esprit.forums.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "forum_reply_votes",
        uniqueConstraints = @UniqueConstraint(name = "uk_reply_vote_user", columnNames = {"reply_id", "user_id"})
)
public class ForumReplyVote {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "reply_id", nullable = false)
        private ForumReply reply;

        @Column(nullable = false, name = "user_id")
        private Long userId;

        @Column(nullable = false, length = 8)
        private String voteType;

        @Column(nullable = false, length = 50)
        private String createdAt;

        @Column(nullable = false, length = 50)
        private String updatedAt;

        public ForumReplyVote() {
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

        public Long getUserId() {
                return userId;
        }

        public void setUserId(Long userId) {
                this.userId = userId;
        }

        public String getVoteType() {
                return voteType;
        }

        public void setVoteType(String voteType) {
                this.voteType = voteType;
        }

        public String getCreatedAt() {
                return createdAt;
        }

        public void setCreatedAt(String createdAt) {
                this.createdAt = createdAt;
        }

        public String getUpdatedAt() {
                return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
                this.updatedAt = updatedAt;
        }
}