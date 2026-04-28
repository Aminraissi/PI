package org.example.gestioninventaire.dtos.response;

import lombok.*;
import org.example.gestioninventaire.enums.StatutCommande;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO retourné au vétérinaire : commande + infos agriculteur enrichies via Feign.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommandeVetResponse {

    // ── Commande ──────────────────────────────────────────────────────────────
    private Long id;
    private Double montantTotal;
    private LocalDateTime dateCommande;
    private StatutCommande statut;

    // ── Agriculteur (récupéré via UserClient) ────────────────────────────────
    private Long agriculteurId;
    private String agriculteurNom;
    private String agriculteurPrenom;
    private String agriculteurEmail;
    private String agriculteurTelephone;
    private String agriculteurCin;

    // ── Items (uniquement ceux du vétérinaire demandeur) ─────────────────────
    private List<CommandeItemResponse> items;
}