package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "like_avis",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_like_avis_agriculteur",
                columnNames = {"avis_id", "agriculteur_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeAvis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID de l'agriculteur qui a liké (microservice gestion-user) */
    @Column(name = "agriculteur_id", nullable = false)
    private Long agriculteurId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avis_id", nullable = false)
    private Avis avis;
}
