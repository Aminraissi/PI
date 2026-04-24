package org.example.gestioninventaire.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.gestioninventaire.enums.MovementType;
import org.example.gestioninventaire.enums.Reason;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private MovementType movementType;

    private Double quantity;
    private LocalDateTime dateMouvement;

    @Enumerated(EnumType.STRING)
    private Reason reason;

    private String note; // détail libre optionnel (ex: "Campagne vaccination #5")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private InventoryProduct product;

    private Long ownerId;
}
