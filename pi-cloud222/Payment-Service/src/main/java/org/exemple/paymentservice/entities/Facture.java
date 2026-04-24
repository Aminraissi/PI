package org.exemple.paymentservice.entities;

import jakarta.persistence.*;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;


@Entity
@Table(name = "factures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFacture;


    @Column(nullable = false, unique = true)
    private String numero;


    @Column(nullable = false)
    private LocalDate date;


    @Column(nullable = false)
    private Double total;

    @Column
    private String pdfUrl;

    @OneToOne(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Paiement paiement;
}

