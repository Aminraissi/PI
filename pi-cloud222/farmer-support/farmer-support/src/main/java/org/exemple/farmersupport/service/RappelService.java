package org.exemple.farmersupport.service;

import org.exemple.farmersupport.entity.Rappel;

import java.util.List;

public interface RappelService {
    Rappel create(Long eventId, Rappel rappel);
    List<Rappel> getAll();
    List<Rappel> getByEvent(Long eventId);
    Rappel getById(Long id);
    Rappel update(Long id, Rappel rappel);
    void delete(Long id);
}