package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;
import org.example.gestioninventaire.enums.ProductCategory;
import java.time.LocalDate;

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
    private LocalDate dateAchat;
    private LocalDate datePeremption;
    private Double prixAchat;
    private String note;

    // Boutique fields
    private Double prixVente;
    private String imageUrl;
    private String description;
    private Boolean enBoutique;
    private Boolean enStock;   // computed: currentQuantity > 0
}
