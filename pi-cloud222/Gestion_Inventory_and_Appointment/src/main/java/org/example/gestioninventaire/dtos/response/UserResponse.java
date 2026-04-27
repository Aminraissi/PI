package org.example.gestioninventaire.dtos.response;

import lombok.Data;

/**
 * Réponse Feign depuis gestion-user.
 * Le champ cin peut être null si le service user ne l'expose pas encore.
 */
@Data
public class UserResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String cin;          // ← nouveau champ
    private String role;
    private String photo;
    private String region;
    private String adresseCabinet;
    private String telephoneCabinet;
}