package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.gestioninventaire.enums.StatutCommande;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "commandes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long agriculteurId;
    private Double montantTotal;
    private LocalDateTime dateCommande;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutCommande statut = StatutCommande.EN_ATTENTE;

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Column(name = "stripe_client_secret", columnDefinition = "TEXT")
    private String stripeClientSecret;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<CommandeItem> items;
}
