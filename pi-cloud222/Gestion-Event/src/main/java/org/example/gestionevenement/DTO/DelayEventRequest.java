package org.example.gestionevenement.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class DelayEventRequest {
    private String reason;
    private String newDateDebut;
    private String newDateFin;
    private String autorisationMunicipale;
}

