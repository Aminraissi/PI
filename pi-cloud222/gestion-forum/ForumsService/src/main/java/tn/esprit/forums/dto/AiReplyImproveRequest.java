package tn.esprit.forums.dto;

public record AiReplyImproveRequest(
        String draft,
        String postTitle,
        String postContent,
        Long groupId
) {
}