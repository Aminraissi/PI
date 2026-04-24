package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateAvailabilityRequest {

    // Set automatiquement depuis le JWT — ne pas envoyer dans le body
    private Long veterinarianId;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotNull
    private Integer slotDurationMinutes;
}
