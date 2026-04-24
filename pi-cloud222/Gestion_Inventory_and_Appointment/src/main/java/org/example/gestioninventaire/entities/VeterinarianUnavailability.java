package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VeterinarianUnavailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long veterinarianId;

    private LocalDate startDate;
    private LocalDate endDate;

    private LocalTime startTime; // nullable si blocage journée entière
    private LocalTime endTime;   // nullable si blocage journée entière

    private Boolean fullDay;     // true = toute la journée

    private Boolean recurringWeekly; // true si répétitif
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek; // ex : SUNDAY

    private String reason; // congé, séminaire...
}
