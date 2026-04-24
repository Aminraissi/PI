package org.example.gestionevenement.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String titre;
    @Column(length = 1000)
    private String description;
    @Enumerated(EnumType.STRING)
    private TypeEvent type;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private float montant;
    private String image;
    private String region;
    private int capaciteMax;
    @Enumerated(EnumType.STRING)
    private StatutEvent statut;
    private int inscrits;
    private String autorisationmunicipale;

    private Double latitude;
    private Double longitude;
    private Boolean geolocated = false;
    private Boolean isValid = false;

    @JsonIgnore
    @OneToMany(mappedBy = "evenement", fetch = FetchType.LAZY)
    private List<Reservation> reservations;

    private Long idOrganisateur;
}

