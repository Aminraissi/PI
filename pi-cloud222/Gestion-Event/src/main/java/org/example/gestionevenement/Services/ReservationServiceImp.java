package org.example.gestionevenement.Services;

import lombok.AllArgsConstructor;
import org.example.gestionevenement.Repositories.EventRepo;
import org.example.gestionevenement.Repositories.ReservationRepo;
import org.example.gestionevenement.entities.EtatPaiement;
import org.example.gestionevenement.entities.Event;
import org.example.gestionevenement.entities.Reservation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;


@Service
@AllArgsConstructor
public class ReservationServiceImp implements IReservation{

    private ReservationRepo reservationRepo;
    private EventRepo eventRepo;

    @Override
    public List<Reservation> getAllReservations() {
        return reservationRepo.findAll();
    }

    @Override
    public Reservation updateReservation(Reservation reservation) {
        return reservationRepo.save(reservation);
    }

    @Override
    public Reservation addReservation(Reservation reservation) {

        Event event = eventRepo.findById(reservation.getEvenement().getId()).orElse(null);
        int placesRestantes = event.getCapaciteMax() - event.getInscrits();

        if (reservation.getNbPlaceReserve() > placesRestantes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough places available");
        }
        reservation.setEtatPaiement(EtatPaiement.PENDING);

        reservation.setEtatPaiement(EtatPaiement.PENDING);
        reservation.setDateInscription(LocalDateTime.now());

        return reservationRepo.save(reservation);
    }

    @Override
    public void removeReservation(int idReservation) {
      reservationRepo.deleteById(idReservation);
    }

    @Override
    public Reservation getReservation(int idReservation) {
        return reservationRepo.findByIdWithEvent(idReservation).orElse(null);
    }

    @Override
    public List<Reservation> getReservationsByEvent(int eventId) {
        return reservationRepo.findByEvenementId(eventId);
    }
}
