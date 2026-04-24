package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.Disponibilite;

import java.util.List;

public interface IDisponibiliteService {

    Disponibilite addDisponibilite(Long locationId, Disponibilite disponibilite);

    Disponibilite updateDisponibilite(Long id, Disponibilite disponibilite);

    void deleteDisponibilite(Long id);

    List<Disponibilite> getAllDisponibilites();

    void updateForLocation(Long locationId, List<Disponibilite> dispos);
}