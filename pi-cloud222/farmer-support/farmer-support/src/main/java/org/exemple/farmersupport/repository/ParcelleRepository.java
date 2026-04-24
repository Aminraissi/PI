package org.exemple.farmersupport.repository;

import org.exemple.farmersupport.entity.Parcelle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParcelleRepository extends JpaRepository<Parcelle, Long> {
    List<Parcelle> findByTerrainIdTerrain(Long terrainId);
}