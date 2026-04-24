package org.exemple.farmersupport.service;

import org.exemple.farmersupport.entity.Parcelle;

import java.util.List;

public interface ParcelleService {
    Parcelle create(Long terrainId, Parcelle parcelle);
    List<Parcelle> getAll();
    List<Parcelle> getByTerrain(Long terrainId);
    Parcelle getById(Long id);
    Parcelle update(Long id, Parcelle parcelle);
    void delete(Long id);
}