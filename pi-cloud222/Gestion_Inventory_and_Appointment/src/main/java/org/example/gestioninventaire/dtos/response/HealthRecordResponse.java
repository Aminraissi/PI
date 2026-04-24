package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
@Data
@Builder
public class HealthRecordResponse {
    private Long id;
    private String maladie;
    private String traitement;
    private LocalDate dateH;
    private AnimalSummaryResponse animal;
}