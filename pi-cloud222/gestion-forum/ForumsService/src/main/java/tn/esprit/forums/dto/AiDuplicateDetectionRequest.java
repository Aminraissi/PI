package tn.esprit.forums.dto;

import java.util.List;

public record AiDuplicateDetectionRequest(
        String title,
        String content,
        List<String> tags,
        Long groupId
) {
}