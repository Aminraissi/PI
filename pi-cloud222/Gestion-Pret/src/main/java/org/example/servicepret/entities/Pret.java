package org.example.servicepret.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pret {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double tauxInteret;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private Double montantTotal;
    private Integer nbEcheances;

    private long agentId;

    @Enumerated(EnumType.STRING)
    private StatutPret statutPret;

    @OneToOne
    private Contrat contrat;

    @OneToMany(mappedBy = "pret", cascade = CascadeType.ALL)
    private List<Echeance> echeances;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "demande_id")
    private DemandePret demande;


}
