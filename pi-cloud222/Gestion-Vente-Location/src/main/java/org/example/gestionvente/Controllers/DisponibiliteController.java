package org.example.gestionvente.Controllers;

import org.example.gestionvente.Entities.Disponibilite;
import org.example.gestionvente.Entities.JourSemaine;
import org.example.gestionvente.Entities.Location;
import org.example.gestionvente.Repositories.DisponibiliteRepo;
import org.example.gestionvente.Repositories.LocationRepo;
import org.example.gestionvente.Services.IDisponibiliteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/disponibilites")
public class DisponibiliteController {

    @Autowired
    private IDisponibiliteService disponibiliteService;
    @Autowired
    private LocationRepo locationRepo;
    @Autowired
    private DisponibiliteRepo disponibiliteRepo;

    @PostMapping("/add/{locationId}")
    public Disponibilite addDisponibilite(
            @PathVariable("locationId") Long locationId,
            @RequestBody Disponibilite disponibilite) {

        return disponibiliteService.addDisponibilite(locationId, disponibilite);
    }

    // UPDATE
    @PutMapping("/update/{id}")
    public Disponibilite update(
            @PathVariable("id") Long id,
            @RequestBody Disponibilite disponibilite) {

        return disponibiliteService.updateDisponibilite(id, disponibilite);
    }

    // DELETE
    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable("id") Long id) {
        disponibiliteService.deleteDisponibilite(id);
    }

    // GET ALL
    @GetMapping("/all")
    public List<Disponibilite> getAll() {
        return disponibiliteService.getAllDisponibilites();
    }

    @GetMapping("/by-location-and-day")
    public List<Disponibilite> getByLocationAndDay(
            @RequestParam("locationId") Long locationId,
            @RequestParam("jourSemaine") JourSemaine jourSemaine) {

        Location location = locationRepo.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        return disponibiliteRepo.findByLocationAndJourSemaine(location, jourSemaine);
    }

    @PutMapping("/location/{id}")
    public void updateForLocation(@PathVariable("id") Long id,
                                  @RequestBody List<Disponibilite> dispos) {
        disponibiliteService.updateForLocation(id, dispos);
    }
}