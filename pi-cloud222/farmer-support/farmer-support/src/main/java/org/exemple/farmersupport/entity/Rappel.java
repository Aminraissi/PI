package org.exemple.farmersupport.entity;

import jakarta.persistence.*;
import lombok.*;
import org.exemple.farmersupport.enums.CanalNotif;

@Entity
@Table(name = "rappel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "evenement")
@EqualsAndHashCode(exclude = "evenement")
public class Rappel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRappel;

    private int delaiAvantMinutes;

    @Enumerated(EnumType.STRING)
    private CanalNotif canal;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private EvenementCalendrier evenement;
}