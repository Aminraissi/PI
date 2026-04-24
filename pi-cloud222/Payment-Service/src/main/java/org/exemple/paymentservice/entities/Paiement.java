package org.exemple.paymentservice.entities;

import jakarta.persistence.*;

import lombok.*;
import org.exemple.paymentservice.enums.MethodePaiement;
import org.exemple.paymentservice.enums.StatutPaiement;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;


@Entity
@Table(name = "paiements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPaiement;


    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false)
    private LocalDateTime datePaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MethodePaiement methode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statut;

    @Column
    private String reference;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false, unique = true)
    @JsonBackReference
    private Facture facture;

    //reference to vente service
    private Long commandeId;

    // reference to user service
    private Long userId;

}

