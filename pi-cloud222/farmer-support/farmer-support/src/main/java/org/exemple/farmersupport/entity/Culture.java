package org.exemple.farmersupport.entity;

import jakarta.persistence.*;
import lombok.*;
import org.exemple.farmersupport.enums.StadeCulture;

import java.time.LocalDate;

@Entity
@Table(name = "culture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "parcelle")
@EqualsAndHashCode(exclude = "parcelle")
public class Culture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCulture;

    private String espece;
    private String variete;
    private LocalDate dateSemis;
    private LocalDate dateRecoltePrevue;

    @Enumerated(EnumType.STRING)
    private StadeCulture stade;

    private String objectif;

    @ManyToOne
    @JoinColumn(name = "parcelle_id")
    private Parcelle parcelle;
}