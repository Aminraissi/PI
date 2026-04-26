package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "commande_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommandeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Référence au produit de l'inventaire */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private InventoryProduct product;

    private Long vetId;
    private String nomProduit;
    private String vetNom;
    private String vetRegion;
    private Double prixUnitaire;
    private Integer quantite;
    private Double sousTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private Commande commande;
}
