package org.example.gestionevenement.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventNearbyDTO {

    private int id;
    private String titre;
    private String description;
    private String type;
    private String statut;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    private String lieu;
    private String region;

    private float montant;
    private String image;

    private int capaciteMax;
    private int inscrits;

    private Double latitude;
    private Double longitude;

    private int fillPercent;

    private Integer distanceMeters;
    private Double distanceKm;
    private Integer durationSeconds;
    private Integer walkMinutes;
    private Integer bikeMinutes;
    private Integer carMinutes;

    private Double score;
}