package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BlockDayRequest {

    @NotNull
    private Long veterinarianId;

    @NotNull
    private LocalDate date;

    private String reason; // congé, séminaire, etc.
}