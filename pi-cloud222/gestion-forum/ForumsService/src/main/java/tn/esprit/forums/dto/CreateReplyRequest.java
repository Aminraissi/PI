package tn.esprit.forums.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateReplyRequest(
        @NotBlank @Size(min = 12, max = 5000) String content,
        List<@NotBlank String> mediaUrls,
        Long authorId
) {
}
