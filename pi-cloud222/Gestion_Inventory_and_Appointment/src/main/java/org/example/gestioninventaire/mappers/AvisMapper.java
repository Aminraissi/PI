package org.example.gestioninventaire.mappers;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.response.AvisResponse;
import org.example.gestioninventaire.dtos.response.CommentaireAvisResponse;
import org.example.gestioninventaire.dtos.response.ReponseAvisResponse;
import org.example.gestioninventaire.entities.Avis;
import org.example.gestioninventaire.entities.CommentaireAvis;
import org.example.gestioninventaire.entities.ReponseAvis;
import org.example.gestioninventaire.feigns.UserClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AvisMapper {

    private final UserClient userClient;

    // ── Avis complet ─────────────────────────────────────────────

    /**
     * Convertit un Avis en AvisResponse.
     *
     * @param avis         l'entité avis
     * @param currentUserId l'ID de l'utilisateur JWT courant (pour likedByMe)
     */
    public AvisResponse toAvisResponse(Avis avis, Long currentUserId) {
        if (avis == null) return null;

        // Récupérer les infos de l'agriculteur depuis gestion-user
        String agriculteurNom = "";
        String agriculteurPrenom = "";
        String agriculteurPhoto = null;
        try {
            var user = userClient.getUserById(avis.getAgriculteurId());
            if (user != null) {
                agriculteurNom    = user.getNom()    != null ? user.getNom()    : "";
                agriculteurPrenom = user.getPrenom() != null ? user.getPrenom() : "";
                agriculteurPhoto  = user.getPhoto();
            }
        } catch (Exception ignored) {
            // Le microservice user peut être indisponible : on continue sans crash
        }

        // Détecter si l'utilisateur courant a liké cet avis
        boolean likedByMe = currentUserId != null && avis.getLikes().stream()
                .anyMatch(l -> l.getAgriculteurId().equals(currentUserId));

        return AvisResponse.builder()
                .id(avis.getId())
                .note(avis.getNote())
                .commentaire(avis.getCommentaire())
                .agriculteurId(avis.getAgriculteurId())
                .agriculteurNom(agriculteurNom)
                .agriculteurPrenom(agriculteurPrenom)
                .agriculteurPhoto(agriculteurPhoto)
                .veterinarianId(avis.getVeterinarianId())
                .createdAt(avis.getCreatedAt())
                .reponseVet(toReponseResponse(avis.getReponseVet()))
                .commentaires(toCommentaireList(avis.getCommentaires()))
                .nbLikes(avis.getLikes().size())
                .likedByMe(likedByMe)
                .build();
    }

    // ── Réponse vétérinaire ──────────────────────────────────────

    public ReponseAvisResponse toReponseResponse(ReponseAvis reponse) {
        if (reponse == null) return null;

        String vetNom = "";
        String vetPrenom = "";
        String vetPhoto = null;
        try {
            var user = userClient.getUserById(reponse.getVeterinarianId());
            if (user != null) {
                vetNom    = user.getNom()    != null ? user.getNom()    : "";
                vetPrenom = user.getPrenom() != null ? user.getPrenom() : "";
                vetPhoto  = user.getPhoto();
            }
        } catch (Exception ignored) {}

        return ReponseAvisResponse.builder()
                .id(reponse.getId())
                .contenu(reponse.getContenu())
                .veterinarianId(reponse.getVeterinarianId())
                .vetNom(vetNom)
                .vetPrenom(vetPrenom)
                .vetPhoto(vetPhoto)
                .createdAt(reponse.getCreatedAt())
                .build();
    }

    // ── Commentaires agriculteurs ────────────────────────────────

    public CommentaireAvisResponse toCommentaireResponse(CommentaireAvis commentaire) {
        if (commentaire == null) return null;

        String agriNom = "";
        String agriPrenom = "";
        String agriPhoto = null;
        try {
            var user = userClient.getUserById(commentaire.getAgriculteurId());
            if (user != null) {
                agriNom    = user.getNom()    != null ? user.getNom()    : "";
                agriPrenom = user.getPrenom() != null ? user.getPrenom() : "";
                agriPhoto  = user.getPhoto();
            }
        } catch (Exception ignored) {}

        return CommentaireAvisResponse.builder()
                .id(commentaire.getId())
                .contenu(commentaire.getContenu())
                .agriculteurId(commentaire.getAgriculteurId())
                .agriculteurNom(agriNom)
                .agriculteurPrenom(agriPrenom)
                .agriculteurPhoto(agriPhoto)
                .createdAt(commentaire.getCreatedAt())
                .build();
    }

    public List<CommentaireAvisResponse> toCommentaireList(List<CommentaireAvis> commentaires) {
        if (commentaires == null) return List.of();
        return commentaires.stream()
                .map(this::toCommentaireResponse)
                .collect(Collectors.toList());
    }
}
