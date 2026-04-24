package org.example.servicepret.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServicePret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String description;

    private Double montantMax;
    private Double montantMin;
    private Integer dureeMaxMois;

    private Double tauxInteret;
    private Double tauxPenalite;

    private String criteresEligibilite;
    private String documentsRequis;

    @Column(columnDefinition = "TEXT")
    private String delaiTraitement;

    @OneToMany(mappedBy = "service")
    @JsonIgnore
    private List<DemandePret> demandes;

    private Long agentId;
}
