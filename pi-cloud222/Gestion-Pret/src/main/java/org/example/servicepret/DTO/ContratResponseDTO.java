package org.example.servicepret.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.servicepret.entities.Contrat;
import org.example.servicepret.entities.DemandePret;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContratResponseDTO {
    private Contrat contrat;
    private DemandePret demande;
    private User agriculteur;
}
