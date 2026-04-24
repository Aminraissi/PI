package org.exemple.farmersupport.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.exemple.farmersupport.entity.Terrain;
import org.exemple.farmersupport.repository.TerrainRepository;
import org.exemple.farmersupport.service.TerrainService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TerrainServiceImpl implements TerrainService {

    private final TerrainRepository terrainRepository;

    @Override
    public Terrain create(Terrain terrain) {
        return terrainRepository.save(terrain);
    }

    @Override
    public List<Terrain> getAll() {
        return terrainRepository.findAll();
    }

    @Override
    public Terrain getById(Long id) {
        return terrainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Terrain not found with id: " + id));
    }

    @Override
    public Terrain update(Long id, Terrain terrain) {
        Terrain existing = getById(id);
        existing.setNom(terrain.getNom());
        existing.setSuperficieHa(terrain.getSuperficieHa());
        existing.setLocalisation(terrain.getLocalisation());
        existing.setLatitude(terrain.getLatitude());
        existing.setLongitude(terrain.getLongitude());
        existing.setTypeSol(terrain.getTypeSol());
        existing.setIrrigation(terrain.getIrrigation());
        existing.setSourceEau(terrain.getSourceEau());
        existing.setRemarque(terrain.getRemarque());
        existing.setUserId(terrain.getUserId());
        return terrainRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Terrain existing = getById(id);
        terrainRepository.delete(existing);
    }
}