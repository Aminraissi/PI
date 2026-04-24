package org.example.gestioninventaire.dtos.response;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String role;
    private String photo;
    private String region;
    private String adresseCabinet;
    private String telephoneCabinet;
}