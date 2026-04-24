package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String espece;
    private Double poids;

    @Column(unique = true)
    private String reference;

    private LocalDate dateNaissance;

    /* @ManyToOne(fetch = FetchType.LAZY)
     @JoinColumn(name = "owner_id")
     private User owner;*/
    private Long ownerId;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
    public int getAge() {
        // Logique pour calculer l'âge à partir de dateNaissance
        return Period.between(this.dateNaissance, LocalDate.now()).getYears();
    }
}
