package org.exemple.gestionformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.exemple.gestionformation.enums.TypeResource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ressources")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Ressource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRessource;

    private String titre;

    @Enumerated(EnumType.STRING)
    private TypeResource type;

    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formation_id")
    @JsonIgnore
    private Formation formation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    @JsonIgnore
    private Module module;
}
