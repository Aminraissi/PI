package tn.esprit.forums.dto;

import java.util.List;

public record AiModerationAnalysisResponse(
        String recommendation,
        String confidence,
        String rationale,
        List<String> signals
) {
}