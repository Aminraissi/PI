package org.exemple.paymentservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiement_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long propositionId;
    private Long locationId;
    private Long locataireId;
    private Long agriculteurId;

    private Integer moisNumero;
    private Double montant;
    private LocalDate dateEcheance;

    private String statut; // UNPAID, PAID, FAILED

    private String stripeCustomerId;
    private String stripePaymentMethodId;
    private String stripePaymentIntentId;

    private LocalDateTime datePaiement;

    private Double commissionRate;
    private Double commissionAmount;
    private Double farmerAmount;
}