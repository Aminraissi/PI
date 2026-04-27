package org.example.servicepret.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DemandePretDTO {
        private Long id;
        private Double montantDemande;
        private Integer dureeMois;
        private String serviceName;
        private String farmerName;
        private String farmerLastName;
        private LocalDate dateDemande;
        private Integer scoreSolvabilite;
        private String statut;
        private String decision;
        private String fraudRiskLevel;
        private Integer fraudScore;
        private Boolean fraudConfirmed;
        private String fraudAnalysisResult;
}
