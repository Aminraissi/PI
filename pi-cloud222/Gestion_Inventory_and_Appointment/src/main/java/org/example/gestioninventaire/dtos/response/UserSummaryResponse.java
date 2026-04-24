package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String role;
}
