package org.example.gestionvente.Dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommandeHistoryDto {
    private Long commandeId;
    private Long panierId;
    private LocalDateTime dateCommande;
    private Double montantTotal;
    private String statut;
    private String methodePaiement;

}