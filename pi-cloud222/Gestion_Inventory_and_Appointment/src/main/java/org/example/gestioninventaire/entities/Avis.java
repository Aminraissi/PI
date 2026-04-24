package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "avis",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_avis_agriculteur_veterinaire",
                columnNames = {"agriculteur_id", "veterinarian_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Avis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Note de 1 à 5 étoiles */
    @Column(nullable = false)
    private Integer note;

    /** Commentaire de l'agriculteur */
    @Column(columnDefinition = "TEXT")
    private String commentaire;

    /** ID de l'agriculteur (microservice gestion-user) */
    @Column(name = "agriculteur_id", nullable = false)
    private Long agriculteurId;

    /** ID du vétérinaire évalué (microservice gestion-user) */
    @Column(name = "veterinarian_id", nullable = false)
    private Long veterinarianId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** Réponse officielle du vétérinaire à cet avis (max 1) */
    @OneToOne(mappedBy = "avis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ReponseAvis reponseVet;

    /** Commentaires/réponses d'autres agriculteurs sur cet avis */
    @OneToMany(mappedBy = "avis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommentaireAvis> commentaires = new ArrayList<>();

    /** Likes d'agriculteurs sur cet avis */
    @OneToMany(mappedBy = "avis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LikeAvis> likes = new ArrayList<>();
}
