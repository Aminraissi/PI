package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccinationCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String espece;
    private Integer ageMin;
    private Integer ageMax;

    private LocalDate plannedDate;

    private String status; // PLANNED, IN_PROGRESS, COMPLETED

    private Long ownerId; // agriculteur ciblé par la campagne

    private Long productId; // produit vaccin choisi dans l'inventaire
    private Double dose;    // dose par animal

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    private List<Vaccination> vaccinations;
}
