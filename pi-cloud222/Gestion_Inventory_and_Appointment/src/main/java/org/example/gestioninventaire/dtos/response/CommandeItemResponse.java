package org.example.gestioninventaire.dtos.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommandeItemResponse {
    private Long productId;
    private Long vetId;
    private String nomProduit;
    private String vetNom;
    private String vetRegion;
    private Double prixUnitaire;
    private Integer quantite;
    private Double sousTotal;
}
