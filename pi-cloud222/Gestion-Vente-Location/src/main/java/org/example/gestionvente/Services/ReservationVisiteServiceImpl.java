package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.*;
import org.example.gestionvente.Repositories.DisponibiliteRepo;
import org.example.gestionvente.Repositories.LocationRepo;
import org.example.gestionvente.Repositories.ReservationVisiteRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationVisiteServiceImpl implements IReservationVisiteService {

    @Autowired
    private ReservationVisiteRepo reservationRepository;

    @Autowired
    private LocationRepo locationRepository;

    @Autowired
    private DisponibiliteRepo disponibiliteRepository;


    public JourSemaine convertDay(DayOfWeek day) {
        switch (day) {
            case MONDAY:
                return JourSemaine.LUNDI;
            case TUESDAY:
                return JourSemaine.MARDI;
            case WEDNESDAY:
                return JourSemaine.MERCREDI;
            case THURSDAY:
                return JourSemaine.JEUDI;
            case FRIDAY:
                return JourSemaine.VENDREDI;
            case SATURDAY:
                return JourSemaine.SAMEDI;
            case SUNDAY:
                return JourSemaine.DIMANCHE;
            default:
                throw new RuntimeException("Jour invalide");
        }
    }

    @Override
    public ReservationVisite createReservation(Long locationId, Long userId, ReservationVisite reservation) {

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        JourSemaine jour = convertDay(reservation.getDateVisite().getDayOfWeek());

        List<Disponibilite> disponibilites = disponibiliteRepository
                .findByLocationAndJourSemaine(location, jour);

        if (disponibilites.isEmpty()) {
            throw new RuntimeException("Pas de disponibilité pour ce jour");
        }

        boolean slotExistsInDisponibilite = false;

        for (Disponibilite d : disponibilites) {
            if (!d.isEstActive()) {
                continue;
            }

            boolean exactMatch =
                    reservation.getHeureDebut().equals(d.getHeureDebut()) &&
                            reservation.getHeureFin().equals(d.getHeureFin());

            if (exactMatch) {
                slotExistsInDisponibilite = true;
                break;
            }
        }

        if (!slotExistsInDisponibilite) {
            throw new RuntimeException("Créneau hors disponibilité");
        }

        List<StatutReservation> actifs = List.of(
                StatutReservation.EN_ATTENTE,
                StatutReservation.CONFIRMEE,
                StatutReservation.EN_COURS
        );

        boolean dejaReserve = reservationRepository
                .existsByLocationAndDateVisiteAndHeureDebutAndHeureFinAndStatutIn(
                        location,
                        reservation.getDateVisite(),
                        reservation.getHeureDebut(),
                        reservation.getHeureFin(),
                        actifs
                );

        if (dejaReserve) {
            throw new RuntimeException("Créneau déjà réservé !");
        }

        reservation.setLocation(location);
        reservation.setStatut(StatutReservation.EN_ATTENTE);
        reservation.setIdUser(userId);

        return reservationRepository.save(reservation);
    }


    @Override
    public List<ReservationVisite> getReservationsByUser(Long idUser) {
        return reservationRepository.findByIdUser(idUser);
    }

    @Override
    public void deleteReservation(Long id) {

        ReservationVisite reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        reservation.setStatut(StatutReservation.ANNULEE);

        reservationRepository.save(reservation);
    }

    @Override
    public ReservationVisite updateReservation(Long id, ReservationVisite newReservation) {

        // récupérer réservation existante
        ReservationVisite existing = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        // empêcher update si annulée
        if (existing.getStatut() == StatutReservation.ANNULEE) {
            throw new RuntimeException("Impossible de modifier une réservation annulée");
        }

        Location location = existing.getLocation();

        // validation heures
        if (newReservation.getHeureDebut().isAfter(newReservation.getHeureFin())) {
            throw new RuntimeException("heureDebut doit être avant heureFin");
        }

        // convertir date → jour
        JourSemaine jour = convertDay(newReservation.getDateVisite().getDayOfWeek());

        // vérifier disponibilité
        List<Disponibilite> disponibilites = disponibiliteRepository
                .findByLocationAndJourSemaine(location, jour);

        if (disponibilites.isEmpty()) {
            throw new RuntimeException("Pas de disponibilité pour ce jour");
        }

        boolean valide = false;

        for (Disponibilite d : disponibilites) {

            if (!newReservation.getHeureDebut().isBefore(d.getHeureDebut()) &&
                    !newReservation.getHeureFin().isAfter(d.getHeureFin())) {

                valide = true;
                break;
            }
        }

        if (!valide) {
            throw new RuntimeException("Heure hors disponibilité");
        }

        // vérifier overlap (avec autres réservations actives)
        List<StatutReservation> actifs = List.of(
                StatutReservation.EN_ATTENTE,
                StatutReservation.CONFIRMEE,
                StatutReservation.EN_COURS
        );

        List<ReservationVisite> reservations = reservationRepository
                .findByLocationAndDateVisiteAndStatutIn(
                        location,
                        newReservation.getDateVisite(),
                        actifs
                );

        for (ReservationVisite r : reservations) {

            // ignorer lui-même
            if (!r.getId().equals(id)) {

                boolean overlap = newReservation.getHeureDebut().isBefore(r.getHeureFin()) &&
                        newReservation.getHeureFin().isAfter(r.getHeureDebut());

                if (overlap) {
                    throw new RuntimeException("Créneau déjà réservé !");
                }
            }
        }

        // update champs
        existing.setDateVisite(newReservation.getDateVisite());
        existing.setHeureDebut(newReservation.getHeureDebut());
        existing.setHeureFin(newReservation.getHeureFin());

        return reservationRepository.save(existing);
    }

    @Override
    public void confirmerReservation(Long id) {

        ReservationVisite reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        // règles métier
        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new RuntimeException("Impossible de confirmer une réservation annulée");
        }

        if (reservation.getStatut() == StatutReservation.CONFIRMEE) {
            throw new RuntimeException("Reservation déjà confirmée");
        }

        reservation.setStatut(StatutReservation.CONFIRMEE);

        reservationRepository.save(reservation);
    }

    @Override
    public void refuserReservation(Long id) {

        ReservationVisite reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));


        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new RuntimeException("Reservation déjà annulée");
        }

        if (reservation.getStatut() == StatutReservation.CONFIRMEE) {
            throw new RuntimeException("Impossible de refuser une réservation déjà confirmée");
        }

        reservation.setStatut(StatutReservation.ANNULEE);

        reservationRepository.save(reservation);
    }

    @Override
    public void updateExpiredReservations() {
        List<ReservationVisite> reservations = reservationRepository.findAll();

        LocalDateTime now = LocalDateTime.now();

        for (ReservationVisite r : reservations) {
            if (r.getDateVisite() == null || r.getHeureFin() == null) {
                continue;
            }

            LocalDateTime reservationEnd = LocalDateTime.of(
                    r.getDateVisite(),
                    r.getHeureFin()
            );

            if (reservationEnd.isBefore(now)) {
                if (r.getStatut() == StatutReservation.CONFIRMEE) {
                    r.setStatut(StatutReservation.TERMINEE);
                    reservationRepository.save(r);
                } else if (r.getStatut() == StatutReservation.EN_ATTENTE) {
                    r.setStatut(StatutReservation.ANNULEE);
                    reservationRepository.save(r);
                }
            }


        }
    }

    @Override
    public List<ReservationVisite> getReservationsByOwner(Long idUser) {

        List<ReservationVisite> all = reservationRepository.findAll();

        List<ReservationVisite> result = new ArrayList<>();

        for (ReservationVisite r : all) {

            if (r.getLocation() == null) continue;

            if (r.getLocation().getIdUser() == null) continue;

            if (r.getLocation().getIdUser().equals(idUser)) {
                result.add(r);
            }
        }

        return result;
    }

    @Override
    public List<ReservationVisite> getAllReservations() {
        return reservationRepository.findAll();
    }




}
