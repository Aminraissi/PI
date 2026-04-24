package org.exemple.assistance_service.entity;

import org.exemple.assistance_service.enums.StatutReponse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reponse_ingenieur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReponseIngenieur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReponse;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    private LocalDateTime dateReponse;

    @Enumerated(EnumType.STRING)
    private StatutReponse statut;

    @ManyToOne
    @JoinColumn(name = "affectation_id")
    private AffectationDemande affectationDemande;
}