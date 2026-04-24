package org.example.servicepret.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class PretRequestDTO {
    private Double tauxInteret;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Double montantTotal;
    private Integer nbEcheances;
    private Long agentId;
    private Long demandeId;
    private String paymentIntentId;
}
