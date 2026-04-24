package tn.esprit.forums.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotBlank @Size(min = 12, max = 2000) String reason,
        String screenshotDataUrl
) {
}
