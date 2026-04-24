package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AvisResponse {
    private Long id;
    private Integer note;
    private String commentaire;
    private Long agriculteurId;
    private String agriculteurNom;
    private String agriculteurPrenom;
    private String agriculteurPhoto;
    private Long veterinarianId;
    private LocalDateTime createdAt;

    /** Réponse du vétérinaire (null si pas encore répondu) */
    private ReponseAvisResponse reponseVet;

    /** Commentaires des autres agriculteurs */
    private List<CommentaireAvisResponse> commentaires;

    /** Nombre total de likes */
    private int nbLikes;

    /** true si l'utilisateur JWT courant a liké cet avis */
    private boolean likedByMe;
}
