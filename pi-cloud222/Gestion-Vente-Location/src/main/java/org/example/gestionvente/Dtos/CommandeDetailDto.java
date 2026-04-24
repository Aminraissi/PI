package org.example.gestionvente.Dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommandeDetailDto {
    private Long commandeId;
    private Long panierId;
    private LocalDateTime dateCommande;
    private Double montantTotal;
    private String statut;
    private List<CartItemDto> items;

    private Double sousTotal;
    private Double commission;
    private Double tip;
}