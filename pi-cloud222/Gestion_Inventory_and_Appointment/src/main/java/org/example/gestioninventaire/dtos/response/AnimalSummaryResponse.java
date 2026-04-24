package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AnimalSummaryResponse {
    private Long id;
    private String espece;
    private Double poids;
    private String reference;
    private LocalDate dateNaissance;
}
