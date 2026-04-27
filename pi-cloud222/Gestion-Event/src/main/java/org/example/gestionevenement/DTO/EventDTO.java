package org.example.gestionevenement.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {

    private int id;
    private String titre;
    private String type;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private float montant;
    private String image;
    private String region;
    private int capaciteMax;
    private int inscrits;
    private String autorisationmunicipale;
    private Boolean isValid;
    private String delayReason;
}
