package org.example.gestionvente.Controllers;

import org.example.gestionvente.Entities.Disponibilite;
import org.example.gestionvente.Entities.Location;
import org.example.gestionvente.Entities.TypeLocation;
import org.example.gestionvente.Services.ILocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    @Autowired
    private ILocationService service;




    @PostMapping
    public Location create(@RequestBody Location location) {
        return service.create(location);
    }

    // GET ALL
    @GetMapping
    public List<Location> getAll() {
        return service.getAll();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public Location getById(@PathVariable("id") Long id) {
        return service.getById(id);
    }

    // GET BY USER
    @GetMapping("/user/{userId}")
    public List<Location> getByUser(@PathVariable("userId") Long userId) {
        return service.getByUser(userId);
    }

    //  GET BY TYPE
    @GetMapping("/type")
    public List<Location> getByType(@RequestParam("type") TypeLocation type) {
        return service.getByType(type);
    }

    //  GET DISPONIBLE
    @GetMapping("/disponible")
    public List<Location> getDisponibles() {
        return service.getDisponibles();
    }

    //  UPDATE
    @PutMapping("/{id}")
    public Location update(@PathVariable("id") Long id, @RequestBody Location location) {
        return service.update(id, location);
    }

    //  DELETE
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/has-active-reservations")
    public boolean hasActiveReservations(@PathVariable("id") Long id) {
        return service.hasActiveReservations(id);
    }
    @GetMapping("/{id}/disponibilites")
    public List<Disponibilite> getDisponibilitesByLocation(@PathVariable("id") Long id) {
        return service.findByLocationId(id);
    }



}

