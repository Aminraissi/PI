package org.exemple.farmersupport.service;

import org.exemple.farmersupport.entity.Terrain;

import java.util.List;

public interface TerrainService {
    Terrain create(Terrain terrain);
    List<Terrain> getAll();
    Terrain getById(Long id);
    Terrain update(Long id, Terrain terrain);
    void delete(Long id);
}