package tn.esprit.forums.dto;

import java.util.List;

public record AiModerationAnalysisRequest(
        String targetType,
        String title,
        String content,
        List<String> tags,
        Long groupId
) {
}