package org.example.servicepret.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Contrat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateCreation;
    private Double montant;


    private String contenuContrat;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String signatureBase64;
    @Enumerated(EnumType.STRING)
    private StatutContrat statutContrat;

    @ManyToOne
    private DemandePret demande;
}
