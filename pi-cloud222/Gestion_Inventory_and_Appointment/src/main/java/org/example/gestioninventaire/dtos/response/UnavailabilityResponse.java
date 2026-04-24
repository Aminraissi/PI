package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class UnavailabilityResponse {
    private Long id;
    private Long veterinarianId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean fullDay;
    private Boolean recurringWeekly;
    private DayOfWeek dayOfWeek;
    private String reason;
}
