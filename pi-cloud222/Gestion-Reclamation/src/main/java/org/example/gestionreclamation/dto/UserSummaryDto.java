package org.example.gestionreclamation.dto;

public record UserSummaryDto(
        Long id,
        String nom,
        String prenom,
        String email,
        String role
) {
    public String fullName() {
        return ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
    }
}
