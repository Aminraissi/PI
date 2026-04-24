package tn.esprit.forums.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ForumUser {

        private Long id;
        private String name;
        private int reputation;
        private boolean isExpert;
        private String badgeTier;
        private String badgeLabel;

        public ForumUser() {
        }

        public ForumUser(String name, int reputation, boolean isExpert) {
                this.name = name;
                this.reputation = reputation;
                this.isExpert = isExpert;
        }

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

        public int getReputation() {
                return reputation;
        }

        public void setReputation(int reputation) {
                this.reputation = reputation;
        }

        @JsonProperty("isExpert")
        public boolean isExpert() {
                return isExpert;
        }

        public void setExpert(boolean expert) {
                isExpert = expert;
        }

        public String getBadgeTier() {
                return badgeTier;
        }

        public void setBadgeTier(String badgeTier) {
                this.badgeTier = badgeTier;
        }

        public String getBadgeLabel() {
                return badgeLabel;
        }

        public void setBadgeLabel(String badgeLabel) {
                this.badgeLabel = badgeLabel;
        }
}
