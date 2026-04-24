package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VeterinarianAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private Integer bookedSlots;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "veterinarian_id")
//    private User veterinarian;
private Long veterinarianId;
}
