package org.exemple.gestionformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lecon_videos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LeconVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idLecon;

    private String titre;
    private String urlVideo;
    private Integer dureeSecondes;
    private Integer ordre;
    private Boolean estGratuitePreview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    @JsonIgnore
    private Module module;
}