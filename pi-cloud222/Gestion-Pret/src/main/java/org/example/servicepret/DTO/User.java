package org.example.servicepret.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private String cin;
}