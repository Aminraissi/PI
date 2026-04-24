package tn.esprit.forums.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forum_groups")
public class ForumGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(nullable = false, length = 600)
    private String description;

    @Column(nullable = false, length = 50)
    private String createdAt;

    @Column(nullable = false)
    private Long createdBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "forum_group_focus_tags", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "tag", nullable = false, length = 40)
    private List<String> focusTags = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "forum_group_rules", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "rule_text", nullable = false, length = 300)
    private List<String> rules = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "forum_group_member_ids", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "member_id", nullable = false)
    private List<Long> memberIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "forum_group_moderator_ids", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "moderator_id", nullable = false)
    private List<Long> moderatorIds = new ArrayList<>();

    @Transient
    private int memberCount;

    @Transient
    private boolean joined;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public List<String> getFocusTags() {
        return focusTags;
    }

    public void setFocusTags(List<String> focusTags) {
        this.focusTags = focusTags;
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }

    public List<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds;
    }

    public List<Long> getModeratorIds() {
        return moderatorIds;
    }

    public void setModeratorIds(List<Long> moderatorIds) {
        this.moderatorIds = moderatorIds;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public boolean isJoined() {
        return joined;
    }

    public void setJoined(boolean joined) {
        this.joined = joined;
    }
}