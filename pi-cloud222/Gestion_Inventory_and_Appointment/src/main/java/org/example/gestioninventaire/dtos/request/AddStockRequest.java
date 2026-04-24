package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AddStockRequest {

    @NotNull
    @Positive
    private Double quantity;

    @NotNull
    @PositiveOrZero
    private Double price;

    @NotNull
    private LocalDate purchaseDate;

    private LocalDate expiryDate;

    private String note; // détail optionnel
}
