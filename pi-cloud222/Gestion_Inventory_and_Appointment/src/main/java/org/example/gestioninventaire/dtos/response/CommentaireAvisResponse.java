package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentaireAvisResponse {
    private Long id;
    private String contenu;
    private Long agriculteurId;
    private String agriculteurNom;
    private String agriculteurPrenom;
    private String agriculteurPhoto;
    private LocalDateTime createdAt;
}
