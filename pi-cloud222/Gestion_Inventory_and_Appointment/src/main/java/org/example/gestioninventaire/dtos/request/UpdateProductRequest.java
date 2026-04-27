package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.example.gestioninventaire.enums.ProductCategory;
import java.time.LocalDate;

@Data
public class UpdateProductRequest {

    @NotNull
    private String nom;

    @NotNull
    private ProductCategory categorie;

    @NotNull
    private String unit;

    @NotNull
    private Boolean isPerishable;

    @NotNull
    @PositiveOrZero
    private Double currentQuantity;

    @NotNull
    @PositiveOrZero
    private Double minThreshold;

    // Boutique fields (optional)
    private Double prixVente;
    private String imageUrl;
    private String description;
    private Boolean enBoutique;
    private LocalDate dateAchat;
    private LocalDate datePeremption;
    private Double prixAchat;
    private String note;
}
