package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;
import org.example.gestioninventaire.enums.PostType;

import java.time.LocalDateTime;

@Data
@Builder
public class VetPostResponse {
    private Long id;
    private Long veterinarianId;
    private String titre;
    private String description;
    private PostType type;
    private String mediaUrl;
    private String mediaFileName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}