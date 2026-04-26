package org.exemple.gestionformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lecon_commentaires")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LeconCommentaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCommentaire;

    @Column(length = 1000)
    private String contenu;

    private Long auteurId;
    private String auteurNom;
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecon_id")
    @JsonIgnore
    private LeconVideo lecon;
}
