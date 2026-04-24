package org.exemple.farmersupport.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemple.farmersupport.enums.TypeIrrigation;
import org.exemple.farmersupport.enums.TypeSol;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "terrain")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "parcelles")
@EqualsAndHashCode(exclude = "parcelles")
public class Terrain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTerrain;

    private String nom;
    private double superficieHa;
    private String localisation;
    private double latitude;
    private double longitude;

    @Enumerated(EnumType.STRING)
    private TypeSol typeSol;

    @Enumerated(EnumType.STRING)
    private TypeIrrigation irrigation;

    private String sourceEau;
    private String remarque;
    private Long userId;

    @OneToMany(mappedBy = "terrain", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Parcelle> parcelles = new ArrayList<>();
}