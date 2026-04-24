package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.gestioninventaire.enums.AppointmentStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateHeure;
    private String motif;
    private String reason;
    private String refusalReason;
    private LocalDateTime createdAt;
    private LocalDateTime smsReminderSentAt;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus appointmentStatus;

    //    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "farmer_id")
//    private User farmer;
    private Long farmerId;

    //    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "veterinarian_id")
//    private User veterinarian;
    private Long veterinarianId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", unique = true)
    private TimeSlot timeSlot;
}