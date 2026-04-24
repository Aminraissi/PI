package org.example.gestioninventaire.dtos.request;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateVaccinationRequest {

    @NotBlank
    private String vaccin;

    @NotNull
    private LocalDate dateVaccin;

    @NotNull
    @Positive
    private Double dose;
}