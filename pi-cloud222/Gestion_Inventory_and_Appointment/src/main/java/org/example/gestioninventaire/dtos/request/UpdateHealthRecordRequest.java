package org.example.gestioninventaire.dtos.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateHealthRecordRequest {

    @NotBlank
    private String maladie;

    @NotBlank
    private String traitement;

    @NotNull
    private LocalDate dateH;
}
