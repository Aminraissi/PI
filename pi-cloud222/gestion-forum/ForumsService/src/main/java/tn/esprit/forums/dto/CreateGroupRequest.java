package tn.esprit.forums.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateGroupRequest(
        @NotBlank @Size(min = 4, max = 80) String name,
        @NotBlank @Size(min = 14, max = 600) String description,
        @NotEmpty List<@NotBlank String> focusTags,
        List<@NotBlank String> rules
) {
}