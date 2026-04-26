package org.example.gestionvente.Services;

import org.example.gestionvente.Dtos.ClientSignatureRequest;
import org.example.gestionvente.Dtos.ContractInfoRequest;
import org.example.gestionvente.Dtos.PropositionLocationRequest;
import org.example.gestionvente.Entities.PropositionLocation;
import org.example.gestionvente.Entities.ReservationVisite;
import org.example.gestionvente.Entities.Location;
import org.example.gestionvente.Entities.StatutReservation;
import org.example.gestionvente.Repositories.PropositionLocationRepo;
import org.example.gestionvente.Repositories.ReservationVisiteRepo;
import org.example.gestionvente.Repositories.LocationRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
public class PropositionLocationServiceImpl implements IPropositionLocationService {

    private final PropositionLocationRepo propositionRepo;
    private final ReservationVisiteRepo reservationRepo;
    private final LocationRepo locationRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    public PropositionLocationServiceImpl(
            PropositionLocationRepo propositionRepo,
            ReservationVisiteRepo reservationRepo,
            LocationRepo locationRepo
    ) {
        this.propositionRepo = propositionRepo;
        this.reservationRepo = reservationRepo;
        this.locationRepo = locationRepo;
    }

    @Override
    public PropositionLocation createFromReservation(Long reservationId, Long userId, PropositionLocationRequest request) {
        ReservationVisite reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (reservation.getIdUser() == null || !reservation.getIdUser().equals(userId)) {
            throw new RuntimeException("You can only create a proposal for your own reservation");
        }

        StatutReservation statut = reservation.getStatut();

        if (statut != StatutReservation.TERMINEE) {
            throw new RuntimeException("Only completed reservations can create a rental proposal");
        }

        if (propositionRepo.existsByReservationId(reservationId)) {
            throw new RuntimeException("A rental proposal already exists for this reservation");
        }

        Location location = reservation.getLocation();
        if (location == null) {
            throw new RuntimeException("Reservation location not found");
        }

        LocalDate dateDebut = request.getDateDebut();
        LocalDate dateFin = request.getDateFin();

        validateDates(location, dateDebut, dateFin);

        int nbMois = calculateWholeMonths(dateDebut, dateFin);

        PropositionLocation proposition = new PropositionLocation();
        proposition.setReservationId(reservation.getId());
        proposition.setLocationId(location.getId());
        proposition.setLocataireId(userId);
        proposition.setAgriculteurId(location.getIdUser());
        proposition.setDateDebut(dateDebut);
        proposition.setDateFin(dateFin);
        proposition.setNbMois(nbMois);
        proposition.setMontantMensuel(location.getPrix());
        proposition.setMontantTotal(location.getPrix() * nbMois);
        proposition.setStatut("EN_ATTENTE");
        proposition.setDateCreation(LocalDateTime.now());

        return propositionRepo.save(proposition);
    }

    @Override
    public List<PropositionLocation> getByLocataire(Long userId) {
        return propositionRepo.findByLocataireId(userId);
    }

    @Override
    public List<PropositionLocation> getByAgriculteur(Long userId) {
        return propositionRepo.findByAgriculteurId(userId);
    }

    private void validateDates(Location location, LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            throw new RuntimeException("Start date and end date are required");
        }

        if (!dateFin.isAfter(dateDebut)) {
            throw new RuntimeException("End date must be after start date");
        }

        LocalDate dispoDebut = location.getDateDebutLocation();
        LocalDate dispoFin = location.getDateFinLocation();

        if (dispoDebut != null && dateDebut.isBefore(dispoDebut)) {
            throw new RuntimeException("Start date must be within rental availability");
        }

        if (dispoFin != null && dateFin.isAfter(dispoFin)) {
            throw new RuntimeException("End date must be within rental availability");
        }

        int nbMois = calculateWholeMonths(dateDebut, dateFin);

        if (nbMois < 1) {
            throw new RuntimeException("Minimum rental duration is 1 month");
        }
    }

    private int calculateWholeMonths(LocalDate dateDebut, LocalDate dateFin) {
        Period period = Period.between(dateDebut, dateFin);

        if (period.getDays() != 0) {
            throw new RuntimeException("Rental duration must be in whole months only");
        }

        int totalMonths = period.getYears() * 12 + period.getMonths();

        if (totalMonths < 1) {
            throw new RuntimeException("Rental duration must be at least 1 month");
        }

        return totalMonths;
    }

    @Override
    public PropositionLocation acceptProposal(Long proposalId, Long agriculteurId) {
        PropositionLocation proposition = propositionRepo.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        if (!proposition.getAgriculteurId().equals(agriculteurId)) {
            throw new RuntimeException("You can only manage your own proposals");
        }

        if (!"EN_ATTENTE".equals(proposition.getStatut())) {
            throw new RuntimeException("Only pending proposals can be accepted");
        }

        proposition.setStatut("ACCEPTEE");
        proposition.setDateReponse(LocalDateTime.now());

        return propositionRepo.save(proposition);
    }

    @Override
    public PropositionLocation refuseProposal(Long proposalId, Long agriculteurId, String messageRefus) {
        PropositionLocation proposition = propositionRepo.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        if (!proposition.getAgriculteurId().equals(agriculteurId)) {
            throw new RuntimeException("You can only manage your own proposals");
        }

        if (!"EN_ATTENTE".equals(proposition.getStatut())) {
            throw new RuntimeException("Only pending proposals can be refused");
        }

        proposition.setStatut("REFUSEE");
        proposition.setDateReponse(LocalDateTime.now());
        proposition.setMessageRefus(messageRefus);

        return propositionRepo.save(proposition);
    }


    @Override
    public PropositionLocation saveContractInfo(Long proposalId, Long agriculteurId, ContractInfoRequest request) {
        PropositionLocation proposition = propositionRepo.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        if (!proposition.getAgriculteurId().equals(agriculteurId)) {
            throw new RuntimeException("You can only update your own proposals");
        }

        if (!"ACCEPTEE".equals(proposition.getStatut())) {
            throw new RuntimeException("Contract info can only be added after proposal acceptance");
        }

        if (request.getSignatureAgriculteur() == null || request.getSignatureAgriculteur().trim().isEmpty()) {
            throw new RuntimeException("Farmer signature is required");
        }

        proposition.setSignatureAgriculteur(request.getSignatureAgriculteur());
        proposition.setClausesContrat(request.getClausesContrat());
        proposition.setStatut("PRETE_POUR_CONTRAT");

        return propositionRepo.save(proposition);
    }

    @Override
    public PropositionLocation getById(Long proposalId) {
        return propositionRepo.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));
    }


    @Override
    public PropositionLocation signContractByClient(Long proposalId, Long locataireId, ClientSignatureRequest request) {
        PropositionLocation proposition = propositionRepo.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        if (!proposition.getLocataireId().equals(locataireId)) {
            throw new RuntimeException("You can only sign your own contract");
        }

        if (!"PRETE_POUR_CONTRAT".equals(proposition.getStatut())) {
            throw new RuntimeException("Contract is not ready for client signature");
        }

        if (request.getSignatureClient() == null || request.getSignatureClient().trim().isEmpty()) {
            throw new RuntimeException("Client signature is required");
        }

        proposition.setSignatureClient(request.getSignatureClient());

        proposition.setStatut("FINALISEE");

        PropositionLocation saved = propositionRepo.save(proposition);

        createRentalPaymentPlan(saved);

        return saved;
    }


    @Override
    public List<PropositionLocation> getByLocation(Long locationId) {
        return propositionRepo.findByLocationId(locationId);
    }


    @Override
    public boolean hasFinalizedPropositionForUserAndLocation(Long userId, Long locationId) {
        List<PropositionLocation> propositions =
                propositionRepo.findByLocataireIdAndLocationId(userId, locationId);

        return propositions.stream().anyMatch(p ->
                p.getStatut() != null && p.getStatut().equalsIgnoreCase("FINALISEE")
        );
    }

    private void createRentalPaymentPlan(PropositionLocation proposition) {
        try {
            Map<String, Object> body = new HashMap<>();

            body.put("propositionId", proposition.getId());
            body.put("locationId", proposition.getLocationId());
            body.put("locataireId", proposition.getLocataireId());
            body.put("agriculteurId", proposition.getAgriculteurId());
            body.put("nbMois", proposition.getNbMois());
            body.put("montantMensuel", proposition.getMontantMensuel());
            body.put("dateDebut", proposition.getDateDebut().toString());

            String url = "http://localhost:8089/paiement/api/v1/rental-payments/plan";

            restTemplate.postForObject(url, body, Object.class);

            System.out.println("Rental payment plan created for proposition " + proposition.getId());

        } catch (Exception e) {
            System.out.println("Could not create rental payment plan: " + e.getMessage());
        }
    }

    @Override
    public List<PropositionLocation> getAll() {
        return propositionRepo.findAll();
    }
}