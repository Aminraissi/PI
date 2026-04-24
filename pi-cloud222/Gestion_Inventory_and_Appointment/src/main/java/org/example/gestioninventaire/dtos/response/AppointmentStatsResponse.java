package org.example.gestioninventaire.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentStatsResponse {
    private long totalAppointments;
    private long pendingAppointments;
    private long acceptedAppointments;
    private long refusedAppointments;
    private long cancelledAppointments;
    private long todayAppointments;
    private long upcomingAppointments;
    private long distinctAnimals;
}