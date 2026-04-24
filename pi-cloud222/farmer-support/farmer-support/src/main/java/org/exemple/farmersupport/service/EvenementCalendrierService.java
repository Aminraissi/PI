package org.exemple.farmersupport.service;

import org.exemple.farmersupport.entity.EvenementCalendrier;

import java.util.List;

public interface EvenementCalendrierService {
    EvenementCalendrier create(EvenementCalendrier evenement);
    List<EvenementCalendrier> getAll();
    EvenementCalendrier getById(Long id);
    EvenementCalendrier update(Long id, EvenementCalendrier evenement);
    void delete(Long id);
}