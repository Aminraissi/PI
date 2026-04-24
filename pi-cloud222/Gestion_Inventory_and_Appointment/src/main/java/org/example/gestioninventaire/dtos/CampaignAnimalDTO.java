package org.example.gestioninventaire.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CampaignAnimalDTO {

    private Long vaccinationId;

    private Long animalId;
    private String animalName;
    private String animalReference;

    private String status; // PENDING, DONE

    private LocalDate dateVaccin;
}
