package org.example.gestionvente.Entities;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reservationvisite")
public class ReservationVisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateVisite;

    private LocalTime heureDebut;
    private LocalTime heureFin;

    @Enumerated(EnumType.STRING)
    private StatutReservation statut;



    private Long idUser;

    @ManyToOne
    @JoinColumn(name = "idLocation")
    private Location location;


    public Long getId() { return id; }

    public LocalDate getDateVisite() { return dateVisite; }
    public void setDateVisite(LocalDate dateVisite) { this.dateVisite = dateVisite; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    public StatutReservation getStatut() { return statut; }
    public void setStatut(StatutReservation statut) { this.statut = statut; }


    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public Long getIdUser() { return idUser; }
    public void setIdUser(Long idUser) { this.idUser = idUser; }
}