package org.example.gestionevenement.RestController;

import lombok.AllArgsConstructor;
import org.example.gestionevenement.Services.ITicket;
import org.example.gestionevenement.entities.Ticket;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/ticket")
public class TicketController {

    private ITicket iticket;

    @GetMapping("/getAllTickets")
    public List<Ticket> getAllTickets() {
        return iticket.getAllTickets();
    }

    @GetMapping("/getTicket/{id}")
    public Ticket getTicket(@PathVariable int id) {
        return iticket.getTicket(id);
    }

    @PostMapping("/addTicket")
    public Ticket addTicket(@RequestBody Ticket ticket) {
        return iticket.addTicket(ticket);
    }

    @PutMapping("/updateTicket")
    public Ticket updateTicket(@RequestBody Ticket ticket) {
        return iticket.updateTicket(ticket);
    }

    @DeleteMapping("/deleteTicket/{id}")
    public void deleteTicket(@PathVariable int id) {
        iticket.removeTicket(id);
    }

}
