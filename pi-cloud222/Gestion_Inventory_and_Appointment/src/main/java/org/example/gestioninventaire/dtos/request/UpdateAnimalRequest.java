package org.example.gestioninventaire.dtos.request;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateAnimalRequest {

    @NotBlank
    private String espece;

    @NotNull
    @Positive
    private Double poids;

    @NotBlank
    private String reference;

    @NotNull
    private LocalDate dateNaissance;
}
