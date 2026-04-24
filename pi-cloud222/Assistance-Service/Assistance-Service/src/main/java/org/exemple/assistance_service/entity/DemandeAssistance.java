package org.exemple.assistance_service.entity;

import org.exemple.assistance_service.enums.CanalTraitement;
import org.exemple.assistance_service.enums.StatutDemande;
import org.exemple.assistance_service.enums.TypeProbleme;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "demande_assistance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeAssistance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDemande;

    @Enumerated(EnumType.STRING)
    private TypeProbleme typeProbleme;

    private String description;

    private String mediaUrl;

    private String localisation;

    private LocalDateTime dateCreation;

    @Enumerated(EnumType.STRING)
    private CanalTraitement canal;

    @Enumerated(EnumType.STRING)
    private StatutDemande statut;

    private Long userId;

    @OneToOne(mappedBy = "demandeAssistance", cascade = CascadeType.ALL)
    private ReponseIA reponseIA;

    @OneToOne(mappedBy = "demandeAssistance", cascade = CascadeType.ALL)
    private AffectationDemande affectationDemande;
}