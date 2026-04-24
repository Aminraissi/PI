package org.example.gestionevenement.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DelayEventRequest {
    private String        reason;
    private LocalDateTime newDateDebut;
    private LocalDateTime newDateFin;
    private String        autorisationMunicipale;
}
