package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.Commande;
import org.example.gestionvente.Entities.CommandeLigne;
import org.example.gestionvente.Entities.Review;
import org.example.gestionvente.Repositories.CommandeLigneRepo;
import org.example.gestionvente.Repositories.CommandeRepo;
import org.example.gestionvente.Repositories.ReviewRepo;
import org.example.gestionvente.Dtos.ReviewEligibilityResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewServiceImpl implements IReviewService {

    @Autowired
    private ReviewRepo reviewRepo;

    @Autowired
    private CommandeRepo commandeRepo;

    @Autowired
    private CommandeLigneRepo commandeLigneRepo;

    @Autowired
    private IPropositionLocationService propositionLocationService;
    // adapt name/type to your actual service bean

    @Override
    public List<Review> getReviews(String targetType, Long targetId) {
        return reviewRepo.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType.toUpperCase(), targetId);
    }

    @Override
    public Review addReview(String targetType, Long targetId, Long userId, Integer rating, String comment) {
        targetType = targetType.toUpperCase();

        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("La note doit être entre 1 et 5");
        }

        boolean alreadyReviewed = reviewRepo
                .findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .isPresent();

        if (alreadyReviewed) {
            throw new RuntimeException("Vous avez déjà ajouté un avis");
        }

        ReviewEligibilityResponse eligibility =
                "PRODUCT".equals(targetType)
                        ? canReviewProduct(targetId, userId)
                        : canReviewRental(targetId, userId);

        if (!eligibility.isCanReview()) {
            throw new RuntimeException(eligibility.getReason());
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setTargetType(targetType);
        review.setTargetId(targetId);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());

        return reviewRepo.save(review);
    }

    @Override
    public ReviewEligibilityResponse canReviewProduct(Long productId, Long userId) {
        boolean alreadyReviewed = reviewRepo
                .findByUserIdAndTargetTypeAndTargetId(userId, "PRODUCT", productId)
                .isPresent();

        if (alreadyReviewed) {
            return new ReviewEligibilityResponse(false, true, "You already reviewed this product.");
        }

        List<Commande> commandes = commandeRepo.findByUserIdAndStatut(userId, "VALIDEE");

        for (Commande commande : commandes) {
            List<CommandeLigne> lignes = commandeLigneRepo.findByCommandeId(commande.getId());

            boolean found = lignes.stream().anyMatch(l ->
                    productId.equals(l.getProduitId())
            );

            if (found) {
                return new ReviewEligibilityResponse(true, false, "Eligible");
            }
        }

        return new ReviewEligibilityResponse(false, false, "You can review only products you bought.");
    }

    @Override
    public ReviewEligibilityResponse canReviewRental(Long rentalId, Long userId) {
        boolean alreadyReviewed = reviewRepo
                .findByUserIdAndTargetTypeAndTargetId(userId, "RENTAL", rentalId)
                .isPresent();

        if (alreadyReviewed) {
            return new ReviewEligibilityResponse(false, true, "You already reviewed this rental.");
        }

        boolean finalizedRentalExists = propositionLocationService
                .hasFinalizedPropositionForUserAndLocation(userId, rentalId);

        if (!finalizedRentalExists) {
            return new ReviewEligibilityResponse(false, false, "You can review only rentals you actually rented.");
        }

        return new ReviewEligibilityResponse(true, false, "Eligible");
    }

    @Override
    public Review updateReview(String targetType, Long targetId, Long userId, Integer rating, String comment) {
        targetType = targetType.toUpperCase();

        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("La note doit être entre 1 et 5");
        }

        if (comment != null && comment.length() > 120) {
            throw new RuntimeException("Comment must be 120 characters or less.");
        }

        Review review = reviewRepo
                .findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());

        return reviewRepo.save(review);
    }


}