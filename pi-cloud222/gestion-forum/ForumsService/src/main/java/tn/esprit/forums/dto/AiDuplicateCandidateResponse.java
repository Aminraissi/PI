package tn.esprit.forums.dto;

public record AiDuplicateCandidateResponse(
        Long id,
        String title,
        int score,
        int replies,
        int views,
        String reason
) {
}