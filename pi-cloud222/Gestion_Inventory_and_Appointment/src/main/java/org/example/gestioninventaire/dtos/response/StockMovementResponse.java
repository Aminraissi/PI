package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;
import org.example.gestioninventaire.enums.MovementType;
import org.example.gestioninventaire.enums.Reason;

import java.time.LocalDateTime;

@Data
@Builder
public class StockMovementResponse {
    private Long id;
    private MovementType movementType;
    private Double quantity;
    private LocalDateTime dateMouvement;
    private Reason reason;
    private String note;       // détail libre optionnel
    private Long productId;
    private String productName;
    private UserSummaryResponse user;
}
