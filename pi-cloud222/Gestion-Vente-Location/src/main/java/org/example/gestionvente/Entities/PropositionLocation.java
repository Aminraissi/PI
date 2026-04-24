package org.example.gestionvente.Entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proposition_location")
public class PropositionLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "locataire_id", nullable = false)
    private Long locataireId;

    @Column(name = "agriculteur_id", nullable = false)
    private Long agriculteurId;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "nb_mois", nullable = false)
    private Integer nbMois;

    @Column(name = "montant_mensuel", nullable = false)
    private Double montantMensuel;

    @Column(name = "montant_total", nullable = false)
    private Double montantTotal;

    @Column(nullable = false)
    private String statut;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    @Column(name = "message_refus")
    private String messageRefus;

    @Column(name = "signature_agriculteur", columnDefinition = "LONGTEXT")
    private String signatureAgriculteur;

    @Column(name = "clauses_contrat", columnDefinition = "TEXT")
    private String clausesContrat;

    @Column(name = "signature_client", columnDefinition = "LONGTEXT")
    private String signatureClient;

    @Column(name = "date_signature_client")
    private LocalDateTime dateSignatureClient;

    private String stripeCustomerId;
    private String stripePaymentMethodId;
    private Boolean autoPaymentEnabled;

    public Long getId() { return id; }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public Long getLocataireId() { return locataireId; }
    public void setLocataireId(Long locataireId) { this.locataireId = locataireId; }

    public Long getAgriculteurId() { return agriculteurId; }
    public void setAgriculteurId(Long agriculteurId) { this.agriculteurId = agriculteurId; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public Integer getNbMois() { return nbMois; }
    public void setNbMois(Integer nbMois) { this.nbMois = nbMois; }

    public Double getMontantMensuel() { return montantMensuel; }
    public void setMontantMensuel(Double montantMensuel) { this.montantMensuel = montantMensuel; }

    public Double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(Double montantTotal) { this.montantTotal = montantTotal; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateReponse() { return dateReponse; }
    public void setDateReponse(LocalDateTime dateReponse) { this.dateReponse = dateReponse; }

    public String getMessageRefus() { return messageRefus; }
    public void setMessageRefus(String messageRefus) { this.messageRefus = messageRefus; }


    public String getSignatureAgriculteur() { return signatureAgriculteur; }
    public void setSignatureAgriculteur(String signatureAgriculteur) { this.signatureAgriculteur = signatureAgriculteur; }

    public String getClausesContrat() { return clausesContrat; }
    public void setClausesContrat(String clausesContrat) { this.clausesContrat = clausesContrat; }

    public String getSignatureClient() {
        return signatureClient;
    }

    public void setSignatureClient(String signatureClient) {
        this.signatureClient = signatureClient;
    }

    public LocalDateTime getDateSignatureClient() {
        return dateSignatureClient;
    }

    public void setDateSignatureClient(LocalDateTime dateSignatureClient) {
        this.dateSignatureClient = dateSignatureClient;
    }
}