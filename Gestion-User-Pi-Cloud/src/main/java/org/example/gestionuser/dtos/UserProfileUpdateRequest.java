package org.example.gestionuser.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    private String nom;
    private String prenom;
    private String photo;
    private String email;
    private String motDePasse;
    private String telephone;
    private String region;
    private String cin;

    private String adresseCabinet;
    private String presentationCarriere;
    private String telephoneCabinet;

    private String agence;
    private String certificatTravail;

    private String nomOrganisation;
    private String logoOrganisation;
    private String description;
}