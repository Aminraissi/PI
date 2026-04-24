package org.exemple.farmersupport.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.exemple.farmersupport.entity.Parcelle;
import org.exemple.farmersupport.entity.Terrain;
import org.exemple.farmersupport.repository.ParcelleRepository;
import org.exemple.farmersupport.repository.TerrainRepository;
import org.exemple.farmersupport.service.ParcelleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParcelleServiceImpl implements ParcelleService {

    private final ParcelleRepository parcelleRepository;
    private final TerrainRepository terrainRepository;

    @Override
    public Parcelle create(Long terrainId, Parcelle parcelle) {
        Terrain terrain = terrainRepository.findById(terrainId)
                .orElseThrow(() -> new RuntimeException("Terrain not found with id: " + terrainId));
        parcelle.setTerrain(terrain);
        return parcelleRepository.save(parcelle);
    }

    @Override
    public List<Parcelle> getAll() {
        return parcelleRepository.findAll();
    }

    @Override
    public List<Parcelle> getByTerrain(Long terrainId) {
        return parcelleRepository.findByTerrainIdTerrain(terrainId);
    }

    @Override
    public Parcelle getById(Long id) {
        return parcelleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parcelle not found with id: " + id));
    }

    @Override
    public Parcelle update(Long id, Parcelle parcelle) {
        Parcelle existing = getById(id);
        existing.setNom(parcelle.getNom());
        existing.setSuperficieHa(parcelle.getSuperficieHa());
        existing.setGeom(parcelle.getGeom());
        return parcelleRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Parcelle existing = getById(id);
        parcelleRepository.delete(existing);
    }
}