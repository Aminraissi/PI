package org.example.gestionuser.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private String prenom;
    @Column(columnDefinition = "TEXT")
    private String photo;
    private String email;
    private String motDePasse;
    private String telephone;
    private LocalDate dateCreation;
    @Enumerated(EnumType.STRING)
    private StatutCompte statutCompte;
    @Enumerated(EnumType.STRING)
    private EmailVerificationStatus emailVerificationStatus;
    @Enumerated(EnumType.STRING)
    private ProfileValidationStatus profileValidationStatus;
    private String motifRefus;
    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean isOnline;
    private LocalDateTime lastSeen;

    private String region;
    // =============================
    //  Expert
    // =============================

    @Column(columnDefinition = "TEXT")
    private String diplomeExpert;
    // =============================
    //  Transporteur
    // =============================
    private String typeVehicule;
    private Double capaciteKg;
    private String numeroPlaque;

    
    // =============================
    //  Agent
    // =============================

    private String agence;
    @Column(columnDefinition = "TEXT")
    private String certificatTravail;
    // =============================
    //  veto
    // =============================
    private String adresseCabinet;
    private String presentationCarriere;
    private String telephoneCabinet;
    // =============================
    //  Organisateur
    // =============================

    private String nom_organisation;
    @Column(columnDefinition = "TEXT")
    private String logo_organisation;
    @Column(columnDefinition = "TEXT")
    private String cin;

    @Column(columnDefinition = "TEXT")
    private String description;
}


