package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class VaccinationResponse {
    private Long id;
    private String vaccin;          // nom du produit
    private Long productId;         // id du produit en stock
    private LocalDate dateVaccin;
    private Double dose;
    private String status;
    private AnimalSummaryResponse animal;
}
