package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAvisRequest {

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "La note minimale est 1")
    @Max(value = 5, message = "La note maximale est 5")
    private Integer note;

    @NotBlank(message = "Le commentaire est obligatoire")
    private String commentaire;

    @NotNull(message = "L'identifiant du vétérinaire est obligatoire")
    private Long veterinarianId;
}
