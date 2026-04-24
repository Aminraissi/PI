package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.example.gestioninventaire.enums.Reason;

@Data
public class ConsumeStockRequest {

    @NotNull
    @Positive
    private Double quantity;

    @NotNull
    private Reason reason;

    private String note; // détail optionnel
}
