package org.example.gestionvente.Controllers;

import org.example.gestionvente.Entities.Review;
import org.example.gestionvente.Services.IReviewService;
import org.example.gestionvente.Dtos.ReviewEligibilityResponse;
import org.example.gestionvente.Dtos.ReviewRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private IReviewService reviewService;

    @GetMapping("/{targetType}/{targetId}")
    public List<Review> getReviews(
            @PathVariable String targetType,
            @PathVariable Long targetId) {
        return reviewService.getReviews(targetType, targetId);
    }

    @GetMapping("/eligibility/{targetType}/{targetId}")
    public ReviewEligibilityResponse getEligibility(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestParam Long userId) {

        if ("PRODUCT".equalsIgnoreCase(targetType)) {
            return reviewService.canReviewProduct(targetId, userId);
        }
        return reviewService.canReviewRental(targetId, userId);
    }

    @PostMapping("/{targetType}/{targetId}")
    public Review addReview(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestBody ReviewRequest request) {

        return reviewService.addReview(
                targetType,
                targetId,
                request.getUserId(),
                request.getRating(),
                request.getComment()
        );
    }

    @PutMapping("/{targetType}/{targetId}")
    public Review updateReview(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestBody ReviewRequest request) {

        return reviewService.updateReview(
                targetType,
                targetId,
                request.getUserId(),
                request.getRating(),
                request.getComment()
        );
    }
}