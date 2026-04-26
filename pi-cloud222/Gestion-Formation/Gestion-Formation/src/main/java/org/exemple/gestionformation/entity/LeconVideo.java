package org.exemple.gestionformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private LocalDateTime liveAt;
    private String streamingRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    @JsonIgnore
    private Module module;

    @OneToMany(mappedBy = "lecon", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LeconCommentaire> commentaires = new ArrayList<>();
}
