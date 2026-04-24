package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReponseAvisResponse {
    private Long id;
    private String contenu;
    private Long veterinarianId;
    private String vetNom;
    private String vetPrenom;
    private String vetPhoto;
    private LocalDateTime createdAt;
}
