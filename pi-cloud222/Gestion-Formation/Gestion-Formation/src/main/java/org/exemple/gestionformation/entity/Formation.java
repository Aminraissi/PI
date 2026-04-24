package org.exemple.gestionformation.entity;

import org.exemple.gestionformation.enums.NiveauFormation;
import org.exemple.gestionformation.enums.StatutFormation;
import org.exemple.gestionformation.enums.TypeFormation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "formations")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFormation;

    private String titre;
    private String description;
    private String thematique;

    @Enumerated(EnumType.STRING)
    private NiveauFormation niveau;

    @Enumerated(EnumType.STRING)
    private TypeFormation type;

    private Double prix;
    private Boolean estPayante;
    private String langue;
    private LocalDate dateCreation;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private StatutFormation statut;

    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Module> modules = new ArrayList<>();

    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Ressource> ressources = new ArrayList<>();

    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InscriptionFormation> inscriptions = new ArrayList<>();

    // reference to user service
    private Long userId;
}
