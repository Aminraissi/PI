package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdjustStockRequest {

    @NotNull
    private Double quantity;

    private String note; // détail optionnel
}
