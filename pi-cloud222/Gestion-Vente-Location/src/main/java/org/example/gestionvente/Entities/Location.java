package org.example.gestionvente.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

@Entity
@Table(name = "location")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idUser")
    private Long idUser;

    private String nom;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TypeLocation type;

    @NotNull
    @Positive
    private Double prix;
    private Boolean disponibilite;

    @NotNull
    @Column(name = "dateDebutLocation")
    private LocalDate dateDebutLocation;

    @NotNull
    @Column(name = "dateFinLocation")
    private LocalDate dateFinLocation;

    @NotBlank
    private String image;

    // terrain
    @Positive
    private Double superficie;
    private String uniteSuperficie;
    private String typeSol;
    private String localisation;

    // materiel
    private String marque;
    private String modele;

    @Enumerated(EnumType.STRING)
    private EtatMateriel etat;

    @Column(nullable = false)
    private Boolean archived = false;


    public Long getId() {
        return id;
    }


    public Long getIdUser() {
        return idUser;
    }

    public void setIdUser(Long idUser) {
        this.idUser = idUser;
    }


    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }


    public TypeLocation getType() {
        return type;
    }

    public void setType(TypeLocation type) {
        this.type = type;
    }


    public Double getPrix() {
        return prix;
    }

    public void setPrix(Double prix) {
        this.prix = prix;
    }


    public Boolean getDisponibilite() {
        return disponibilite;
    }

    public void setDisponibilite(Boolean disponibilite) {
        this.disponibilite = disponibilite;
    }

    public LocalDate getDateDebutLocation() {
        return dateDebutLocation;
    }

    public void setDateDebutLocation(LocalDate dateDebutLocation) {
        this.dateDebutLocation = dateDebutLocation;
    }

    public LocalDate getDateFinLocation() {
        return dateFinLocation;
    }

    public void setDateFinLocation(LocalDate dateFinLocation) {
        this.dateFinLocation = dateFinLocation;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

// ---------- TERRAIN ----------

    public Double getSuperficie() {
        return superficie;
    }

    public void setSuperficie(Double superficie) {
        this.superficie = superficie;
    }

    public String getUniteSuperficie() {
        return uniteSuperficie;
    }

    public void setUniteSuperficie(String uniteSuperficie) {
        this.uniteSuperficie = uniteSuperficie;
    }

    public String getTypeSol() {
        return typeSol;
    }

    public void setTypeSol(String typeSol) {
        this.typeSol = typeSol;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

// ---------- MATERIEL

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getModele() {
        return modele;
    }

    public void setModele(String modele) {
        this.modele = modele;
    }

    public EtatMateriel getEtat() {
        return etat;
    }

    public void setEtat(EtatMateriel etat) {
        this.etat = etat;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }
}