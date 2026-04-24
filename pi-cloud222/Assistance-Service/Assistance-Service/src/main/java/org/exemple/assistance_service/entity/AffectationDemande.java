package org.exemple.assistance_service.entity;

import org.exemple.assistance_service.enums.StatutAffectation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "affectation_demande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationDemande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAffectation;

    private LocalDateTime dateAffectation;

    @Enumerated(EnumType.STRING)
    private StatutAffectation statut;

    private Long ingenieurId;

    @Column(columnDefinition = "TEXT")
    private String ingenieursRefuses;

    @OneToOne
    @JoinColumn(name = "demande_id")
    private DemandeAssistance demandeAssistance;

    @OneToMany(mappedBy = "affectationDemande", cascade = CascadeType.ALL)
    private List<ReponseIngenieur> reponsesIngenieur;
}
