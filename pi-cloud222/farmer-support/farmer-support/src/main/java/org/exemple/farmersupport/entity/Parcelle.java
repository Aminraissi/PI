package org.exemple.farmersupport.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "parcelle")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"terrain", "cultures"})
@EqualsAndHashCode(exclude = {"terrain", "cultures"})
public class Parcelle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idParcelle;

    private String nom;
    private double superficieHa;
    @Column(columnDefinition = "TEXT")
    private String geom;

    @ManyToOne
    @JoinColumn(name = "terrain_id")
    private Terrain terrain;

    @OneToMany(mappedBy = "parcelle", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Culture> cultures = new ArrayList<>();

    @Transient
    private Double surface;

    @JsonProperty("surface")
    public Double getSurface() {
        if (surface != null) return surface;
        return this.superficieHa > 0 ? this.superficieHa * 10000 : null;
    }

    @JsonProperty("surface")
    public void setSurface(Double surface) {
        this.surface = surface;
        if (surface != null) {
            this.superficieHa = surface / 10000.0;
        }
    }
}