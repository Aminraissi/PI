package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.gestioninventaire.enums.SlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isBooked;

    @Enumerated(EnumType.STRING)
    private SlotStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "availability_id")
    private VeterinarianAvailability availability;
}
