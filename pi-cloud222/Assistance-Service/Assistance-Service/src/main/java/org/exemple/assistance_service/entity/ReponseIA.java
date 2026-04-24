package org.exemple.assistance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reponse_ia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReponseIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReponseIA;

    @Column(columnDefinition = "TEXT")
    private String diagnostic;

    private double probabilite;

    @Column(columnDefinition = "TEXT")
    private String recommandations;

    private LocalDateTime dateGeneration;

    private String modele;

    @OneToOne
    @JoinColumn(name = "demande_id")
    private DemandeAssistance demandeAssistance;
}