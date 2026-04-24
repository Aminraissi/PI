package tn.esprit.livraison.client.dto;

public record UserSummaryDto(
        Integer id,
        String role,
        String statusCompte,
        Boolean disponible,
        String nom,
        String prenom
) {
}

