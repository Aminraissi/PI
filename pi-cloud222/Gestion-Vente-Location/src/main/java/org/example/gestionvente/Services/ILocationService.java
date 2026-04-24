package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.Disponibilite;
import org.example.gestionvente.Entities.Location;
import org.example.gestionvente.Entities.TypeLocation;

import java.util.List;

public interface ILocationService {

    Location create(Location location);

    List<Location> getAll();

    Location getById(Long id);

    List<Location> getByUser(Long userId);

    List<Location> getByType(TypeLocation type);

    List<Location> getDisponibles();

    Location update(Long id, Location location);

    void delete(Long id);

    boolean hasActiveReservations(Long locationId);

    List<Disponibilite> findByLocationId(Long locationId);
}