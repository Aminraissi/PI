package org.example.gestionevenement.RestController;

import lombok.AllArgsConstructor;
import org.example.gestionevenement.Services.IReservation;
import org.example.gestionevenement.entities.Reservation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/reservation")
public class ReservationController {

    private IReservation ireservation;

    @GetMapping("/getAllReservations")
    public List<Reservation> getAllReservations() {
        return ireservation.getAllReservations();
    }

    @GetMapping("/getReservation/{id}")

    public Reservation getReservation(@PathVariable int id) {
        return ireservation.getReservation(id);
    }

    @PostMapping("/addReservation")
    public Reservation addReservation(@RequestBody Reservation reservation) {
        System.out.println("Incoming reservation: " + reservation);
        return ireservation.addReservation(reservation);
    }

    @PutMapping("/updateReservation")
    public Reservation updateReservation(@RequestBody Reservation reservation) {
        return ireservation.updateReservation(reservation);
    }

    @DeleteMapping("/deleteReservation/{id}")
    public void deleteReservation(@PathVariable int id) {
        ireservation.removeReservation(id);
    }

}
