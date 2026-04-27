package org.example.gestioninventaire.dtos.request;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommandeRequest {
    private Long agriculteurId;
    private List<CommandeItemRequest> items;
}
