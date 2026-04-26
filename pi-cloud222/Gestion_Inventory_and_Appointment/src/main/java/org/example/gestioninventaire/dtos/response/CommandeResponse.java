package org.example.gestioninventaire.dtos.response;

import lombok.*;
import org.example.gestioninventaire.enums.StatutCommande;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommandeResponse {
    private Long id;
    private Long agriculteurId;
    private Double montantTotal;
    private LocalDateTime dateCommande;
    private StatutCommande statut;
    private String stripeClientSecret;
    private List<CommandeItemResponse> items;
}
