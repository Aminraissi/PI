package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;
import org.example.gestioninventaire.enums.ProductCategory;

@Data
@Builder
public class InventoryProductResponse {
    private Long id;
    private String nom;
    private ProductCategory categorie;
    private String unit;
    private Boolean isPerishable;
    private Double currentQuantity;
    private Double minThreshold;
    private UserSummaryResponse owner;

    // Boutique fields
    private Double prixVente;
    private String imageUrl;
    private String description;
    private Boolean enBoutique;
    private Boolean enStock;   // computed: currentQuantity > 0
}
