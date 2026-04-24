package org.example.gestionuser.dtos;

public record UserSummaryDto(
        Long id,
        String nom,
        String prenom,
        String email
) {
}

