package org.example.gestionvente.Controllers;

import org.example.gestionvente.Entities.ReservationVisite;
import org.example.gestionvente.Repositories.ReservationVisiteRepo;
import org.example.gestionvente.Services.IReservationVisiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
public class ReservationVisiteController {

    @Autowired
    private IReservationVisiteService reservationService;
    @Autowired
    private ReservationVisiteRepo reservationRepository;

    @PostMapping("/add/{locationId}")
    public ReservationVisite create(
            @PathVariable("locationId") Long locationId,
            @RequestParam("userId") Long userId,
            @RequestBody ReservationVisite reservation) {

        return reservationService.createReservation(locationId, userId, reservation);
    }

    @GetMapping("/user/{idUser}")
    public List<ReservationVisite> getByUser(@PathVariable("idUser") Long idUser) {
        return reservationService.getReservationsByUser(idUser);
    }
    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable("id") Long id) {
        reservationService.deleteReservation(id);
    }
    @PutMapping("/update/{id}")
    public ReservationVisite update(
            @PathVariable("id") Long id,
            @RequestBody ReservationVisite reservation) {

        return reservationService.updateReservation(id, reservation);
    }
    @PutMapping("/confirm/{id}")
    public void confirmer(@PathVariable("id") Long id) {
        reservationService.confirmerReservation(id);
    }
    @PutMapping("/refuse/{id}")
    public void refuser(@PathVariable("id") Long id) {
        reservationService.refuserReservation(id);
    }

    @GetMapping("/owner/{idUser}")
    public List<ReservationVisite> getByOwner(@PathVariable("idUser") Long idUser) {
        return reservationService.getReservationsByOwner(idUser);
    }

    @GetMapping("/location/{id}")
    public List<ReservationVisite> getByLocation(@PathVariable("id") Long id) {
        return reservationRepository.findByLocationId(id);
    }

    @GetMapping
    public List<ReservationVisite> getAll() {
        return reservationRepository.findAll();
    }

}
