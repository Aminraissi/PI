package org.example.gestionvente.Entities;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "produitagricole")
public class ProduitAgricole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nom;
    @NotBlank
    @Column(length = 1000)
    private String description;
    @NotNull
    @Positive
    private Double prix;

    @PositiveOrZero
    private Double quantiteDisponible;

    @NotBlank
    private String photoProduit;

    private Long idUser;
    @NotBlank
    private String category;

    public Long getId() {
        return id;
    }

    public Double getPrix() {
        return prix;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getQuantiteDisponible() {
        return quantiteDisponible;
    }

    public void setQuantiteDisponible(Double quantiteDisponible) {
        this.quantiteDisponible = quantiteDisponible;
    }
    public String getPhotoProduit() {
        return photoProduit;
    }
    public void setPhotoProduit(String photoProduit) {
        this.photoProduit = photoProduit;
    }

    public void setPrix(Double prix) {
        this.prix = prix;
    }

    public Long getIdUser() {
        return idUser;
    }

    public void setIdUser(Long idUser) {
        this.idUser = idUser;
    }
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

}