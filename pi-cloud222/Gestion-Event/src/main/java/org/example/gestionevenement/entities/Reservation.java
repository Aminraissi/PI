package org.example.gestionevenement.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int nbPlaceReserve;
    private float montant;
    @Enumerated(EnumType.STRING)
    private EtatPaiement etatPaiement;
    private LocalDateTime dateInscription;
    private String paymentIntentId;

//    @ManyToOne
//    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "evenement_id")
    private Event evenement;

    @JsonIgnore
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL)
    private List<Ticket> tickets;

    private long id_user;
}
