package tn.esprit.forums.dto;

import java.util.List;

public record AiTagSuggestionRequest(
        String title,
        String content,
        List<String> tags,
        Long groupId
) {
}