package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.gestioninventaire.enums.ProductCategory;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @Enumerated(EnumType.STRING)
    private ProductCategory categorie;

    private String unit;
    private Boolean isPerishable;
    private Double currentQuantity;
    private Double minThreshold;

    private Long ownerId;

    // ── Boutique en ligne (nullable) ──────────────────────────────────
    /** Prix de vente affiché aux agriculteurs. Null = non mis en vente */
    private Double prixVente;

    /** URL de l'image du produit */
    private String imageUrl;

    /** Description / notice d'utilisation */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** true = visible dans la boutique publique */
    @Builder.Default
    @Column(nullable = false)
    private Boolean enBoutique = false;
}
