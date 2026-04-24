package org.example.gestioninventaire.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateUnavailabilityRequest {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @NotNull
    private Boolean fullDay;

    @NotNull
    private Boolean recurringWeekly;

    private DayOfWeek dayOfWeek;

    private String reason;
}