package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.Review;
import org.example.gestionvente.Dtos.ReviewEligibilityResponse;

import java.util.List;

public interface IReviewService {
    List<Review> getReviews(String targetType, Long targetId);
    Review addReview(String targetType, Long targetId, Long userId, Integer rating, String comment);
    ReviewEligibilityResponse canReviewProduct(Long productId, Long userId);
    ReviewEligibilityResponse canReviewRental(Long rentalId, Long userId);
    Review updateReview(String targetType, Long targetId, Long userId, Integer rating, String comment);

}