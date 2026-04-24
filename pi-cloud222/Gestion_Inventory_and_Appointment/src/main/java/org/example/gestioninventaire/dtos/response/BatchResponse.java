package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class BatchResponse {
    private Long id;
    private String lotNumber;
    private Double quantity;
    private Double price;
    private LocalDate expiryDate;
    private LocalDate purchaseDate;
    private String note;
}
