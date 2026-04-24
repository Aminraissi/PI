package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class VeterinarianAvailabilityResponse {
    private Long id;
    private LocalDate date;
    private Integer bookedSlots;
    private UserSummaryResponse veterinarian;
    private List<TimeSlotResponse> timeSlots;
}
