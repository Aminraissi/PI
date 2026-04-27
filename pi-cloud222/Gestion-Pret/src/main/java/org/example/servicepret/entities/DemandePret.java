package org.example.servicepret.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DemandePret {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double montantDemande;
    private Integer dureeMois;
    private String objet;

    private LocalDate dateDemande;
    private Integer scoreSolvabilite;
    private String decision;

    private long agriculteurId;

    @OneToMany(mappedBy = "demandePret", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<DemandePretDocument> documents = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private StatutDemande statut;


    @ManyToOne
    @JoinColumn(name = "service_id")
    private ServicePret service;



    @Column(columnDefinition = "TEXT")
    private String encryptedDataKey;

    @Column(columnDefinition = "TEXT")
    private String motifRejet;


    @Column(length = 20)
    private String fraudRiskLevel;

    private Integer fraudScore;

    private Boolean fraudConfirmed;

    @Column(columnDefinition = "TEXT")
    private String fraudAnalysisResult;
}
