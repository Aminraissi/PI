package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.gestioninventaire.enums.PostType;

import java.time.LocalDateTime;

@Entity
@Table(name = "vet_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VetPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID du vétérinaire auteur (microservice gestion-user) */
    @Column(name = "veterinarian_id", nullable = false)
    private Long veterinarianId;

    /** Titre de l'article ou de la vidéo */
    @Column(nullable = false)
    private String titre;

    /** Description / contenu textuel */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Type : ARTICLE ou VIDEO */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type;

    /**
     * Pour ARTICLE : URL ou chemin de l'image de couverture (optionnel).
     * Pour VIDEO   : URL ou chemin du fichier vidéo.
     */
    @Column(name = "media_url")
    private String mediaUrl;

    /** Nom original du fichier (pour l'affichage) */
    @Column(name = "media_file_name")
    private String mediaFileName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}