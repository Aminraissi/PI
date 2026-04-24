package org.example.gestioninventaire.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VaccinationDTO {
    private Long id;
    private Long animalId;
    private Long campaignId;
    private LocalDate dateVaccin;
    private Double dose;
    private String status;
    private LocalDate plannedDate;
}
