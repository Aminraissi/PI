package org.example.gestionvente.Dtos;


public class ReviewEligibilityResponse {
    private boolean canReview;
    private boolean alreadyReviewed;
    private String reason;

    public ReviewEligibilityResponse() {}

    public ReviewEligibilityResponse(boolean canReview, boolean alreadyReviewed, String reason) {
        this.canReview = canReview;
        this.alreadyReviewed = alreadyReviewed;
        this.reason = reason;
    }

    public boolean isCanReview() { return canReview; }
    public void setCanReview(boolean canReview) { this.canReview = canReview; }

    public boolean isAlreadyReviewed() { return alreadyReviewed; }
    public void setAlreadyReviewed(boolean alreadyReviewed) { this.alreadyReviewed = alreadyReviewed; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}