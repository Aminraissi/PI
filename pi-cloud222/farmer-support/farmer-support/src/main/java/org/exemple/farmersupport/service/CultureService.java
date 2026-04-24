package org.exemple.farmersupport.service;

import org.exemple.farmersupport.entity.Culture;

import java.util.List;

public interface CultureService {
    Culture create(Long parcelleId, Culture culture);
    List<Culture> getAll();
    List<Culture> getByParcelle(Long parcelleId);
    Culture getById(Long id);
    Culture update(Long id, Culture culture);
    void delete(Long id);
}