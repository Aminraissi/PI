package org.example.gestionevenement.Repositories;

import org.example.gestionevenement.entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepo extends JpaRepository<Ticket, Integer> {

    Ticket findByCodeTicket(String codeTicket);

    Ticket findByReservationId(int reservationId);
}
