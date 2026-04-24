package tn.esprit.forums.service;

public enum BadgeTier {
    NEW_MEMBER("New Member", 0),
    RISING_MEMBER("Rising Member", 300),
    TRUSTED_CONTRIBUTOR("Trusted Contributor", 800),
    ELITE_GROWER("Elite Grower", 1500);

    private final String label;
    private final int minimumReputation;

    BadgeTier(String label, int minimumReputation) {
        this.label = label;
        this.minimumReputation = minimumReputation;
    }

    public String getLabel() {
        return label;
    }

    public int getMinimumReputation() {
        return minimumReputation;
    }

    public static BadgeTier fromReputation(int reputation) {
        BadgeTier result = NEW_MEMBER;
        for (BadgeTier tier : values()) {
            if (reputation >= tier.minimumReputation) {
                result = tier;
            }
        }
        return result;
    }
}