package org.example.gestionevenement.Services;

import org.example.gestionevenement.entities.Event;
import org.example.gestionevenement.entities.Reservation;
import org.example.gestionevenement.entities.Ticket;

import java.util.List;

public interface ITicket {
    List<Ticket> getAllTickets();
    Ticket updateTicket (Ticket ticket);
    Ticket addTicket (Ticket ticket);
    Ticket getTicket (int idTicket);
    void removeTicket (int idTicket);
    public Ticket generateTicket(Reservation reservation);
}
