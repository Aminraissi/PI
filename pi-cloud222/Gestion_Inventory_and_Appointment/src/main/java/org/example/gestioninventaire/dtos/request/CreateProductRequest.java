package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.example.gestioninventaire.enums.ProductCategory;
import java.time.LocalDate;

@Data
public class CreateProductRequest {

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

    private LocalDate dateAchat;
    private LocalDate datePeremption;
    @PositiveOrZero
    private Double prixAchat;
    private String note;

    private Long ownerId;
}
