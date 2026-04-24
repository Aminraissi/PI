package org.example.gestionevenement.Services;

import org.example.gestionevenement.entities.Event;
import org.example.gestionevenement.entities.Reservation;

import java.util.List;

public interface IReservation {
    List<Reservation> getAllReservations();
    Reservation updateReservation (Reservation reservation);
    Reservation addReservation (Reservation reservation);
    void removeReservation (int idReservation);
    Reservation getReservation(int idReservation);
    List<Reservation> getReservationsByEvent(int eventId);
}
