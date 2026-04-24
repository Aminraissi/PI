package org.example.gestionuser.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupStep2Request {
    private String photo;
    private String telephone;
    private String region;

    private String diplomeExpert;
    private String documentUrl;

    private String vehicule;
    private Double capacite;
    private Double latitudeActuelle;
    private Double longitudeActuelle;
    private Boolean disponible;

    private String agence;
    private String certificatTravail;

    private String organizationLogo;
    private String cin;

    private String adresseCabinet;
    private String presentationCarriere;
    private String telephoneCabinet;

    private String nomOrganisation;
    private String description;
}
