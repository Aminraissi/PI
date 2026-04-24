package org.example.gestioninventaire.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VaccinationCampaignDTO {

    private Long id;

    private String espece;
    private Integer ageMin;
    private Integer ageMax;

    private LocalDate plannedDate;

    private String status; // PLANNED, IN_PROGRESS, COMPLETED

    private Long ownerId; // extrait du JWT

    // L'agriculteur choisit un produit de son inventaire au lieu de taper un nom
    private Long productId;
    private Double dose;

    // Champs retour uniquement (non envoyés dans le body)
    private String productName; // nom du produit, retourné dans la réponse
}
