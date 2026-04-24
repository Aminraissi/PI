package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class CreateAppointmentRequest {

    @NotNull
    private Long veterinarianId;

    @NotNull
    private Long animalId;

    @NotNull
    private Long timeSlotId;

    @NotBlank
    private String motif;

    private String reason;
}
