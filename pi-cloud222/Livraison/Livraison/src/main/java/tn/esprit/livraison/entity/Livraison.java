package tn.esprit.livraison.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.livraison.enums.StatusDemande;
import tn.esprit.livraison.enums.StatusLivraison;
import tn.esprit.livraison.enums.TypeLivraison;

@Entity
@Getter
@Setter
public class Livraison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String reference;

    @Enumerated(EnumType.STRING)
    private TypeLivraison type;

    @Enumerated(EnumType.STRING)
    private StatusLivraison status;

    @Enumerated(EnumType.STRING)
    private StatusDemande statusDemande;

    private int agriculteurId;
    private int transporteurId;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDemande;
    private LocalDateTime dateDepart;
    private LocalDateTime datePreferenceAgriculteur;
    private LocalDateTime dateProposeeNegociation;

    private String adresseDepart;
    private String adresseArrivee;

    private double latDepart;
    private double lngDepart;
    private double latArrivee;
    private double lngArrivee;
    private double latActuelle;
    private double lngActuelle;
    private Double prixNegocie;
    private double distanceKm;

    private Integer livreurIdProposant;
    private String statusNegociation;
    private String messageNegociation;

    private double poids;
    private double quantiteProduit;
    private String uniteProduit;
    private double volume;
    private String typeProduit;

    @Column(length = 1000)
    private String detailsDemande;

    private boolean estRegroupable;

    private double prix;
    private String tarifType;
    private double note;
    private String ratingStatus;
    private LocalDateTime ratingDecisionAt;

    private LocalDateTime dateLivraisonPrevue;
    private LocalDateTime dateLivraisonEffective;

    private int retardMinutes;

    private boolean grouped;
    private String groupReference;
    private LocalDateTime groupedAt;
    private Double prixAvantRegroupement;

    private Integer notificationFromUserId;
    private Integer notificationToUserId;
    private String notificationType;
    private String notificationTitle;

    @Column(length = 1000)
    private String notificationMessage;

    private String notificationStatus;
    private Double proposedPrice;
    private LocalDateTime proposedDateTime;
    private Double minAllowedPrice;
    private Double maxAllowedPrice;
    private LocalDateTime notificationCreatedAt;
    private LocalDateTime notificationHandledAt;
    private boolean notificationSeen;

    private String signatureStatus;

    @Column(columnDefinition = "TEXT")
    private String signatureData;

    private LocalDateTime signedAt;
}