package org.example.gestioninventaire.dtos.request;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommandeItemRequest {
    private Long productId;
    private Long vetId;
    private String nomProduit;
    private String vetNom;
    private String vetRegion;
    private Double prixUnitaire;
    private Integer quantite;
}
