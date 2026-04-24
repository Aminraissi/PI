package tn.esprit.forums.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreatePostRequest(
        @NotBlank @Size(min = 12, max = 200) String title,
        @NotBlank @Size(min = 30, max = 5000) String content,
        @NotEmpty List<@NotBlank String> tags,
        List<@NotBlank String> mediaUrls,
        Long groupId,
        Long authorId,
        Boolean generateAiReply
) {
}
