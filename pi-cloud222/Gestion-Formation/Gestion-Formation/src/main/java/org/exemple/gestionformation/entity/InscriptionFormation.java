package org.exemple.gestionformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemple.gestionformation.enums.StatutAcces;

import java.time.LocalDate;

@Entity
@Table(name = "inscriptions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class InscriptionFormation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idInscription;

    private LocalDate dateInscription;

    @Enumerated(EnumType.STRING)
    private StatutAcces statutAcces;

    private Double progression;

    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formation_id")
    @JsonIgnore
    private Formation formation;


    // external payment service reference
    private Long paiementId;
}