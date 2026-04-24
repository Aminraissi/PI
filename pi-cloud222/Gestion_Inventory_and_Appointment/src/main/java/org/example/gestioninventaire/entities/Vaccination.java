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
public class Vaccination {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String vaccin;       // nom issu du produit en stock
        private LocalDate dateVaccin;
        private Double dose;

        private String status;       // PENDING, DONE

        @ManyToOne(fetch = FetchType.LAZY)
        private Animal animal;

        @ManyToOne
        private VaccinationCampaign campaign;

        // Lien vers le produit vaccin en stock (pour traçabilité et déduction stock)
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "product_id")
        private InventoryProduct product;
}
