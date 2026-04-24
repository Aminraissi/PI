package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;
import org.example.gestioninventaire.enums.AppointmentStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponse {
    private Long id;
    private LocalDateTime dateHeure;
    private String motif;
    private String reason;
    private String refusalReason;
    private LocalDateTime createdAt;
    private AppointmentStatus appointmentStatus;
    private UserSummaryResponse farmer;
    private UserSummaryResponse veterinarian;
    private AnimalSummaryResponse animal;
    private TimeSlotResponse timeSlot;
}
