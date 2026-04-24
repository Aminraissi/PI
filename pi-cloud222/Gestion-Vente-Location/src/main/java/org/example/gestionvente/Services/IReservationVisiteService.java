package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.ReservationVisite;

import java.util.List;

public interface IReservationVisiteService {


    ReservationVisite createReservation(Long locationId, Long userId, ReservationVisite reservation);
    List<ReservationVisite> getReservationsByUser(Long idUser);
    void deleteReservation(Long id);
    ReservationVisite updateReservation(Long id, ReservationVisite newReservation);
    void confirmerReservation(Long id);
    void refuserReservation(Long id);
    void updateExpiredReservations();
    List<ReservationVisite> getReservationsByOwner(Long idUser);
    List<ReservationVisite> getAllReservations();

}