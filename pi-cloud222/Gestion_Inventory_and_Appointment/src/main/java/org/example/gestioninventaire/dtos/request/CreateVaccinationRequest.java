package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateVaccinationRequest {

    // L'agriculteur choisit un produit de son inventaire
    @NotNull
    private Long productId;

    @NotNull
    private LocalDate dateVaccin;

    @NotNull
    @Positive
    private Double dose;

    @NotNull
    private Long animalId;
}
