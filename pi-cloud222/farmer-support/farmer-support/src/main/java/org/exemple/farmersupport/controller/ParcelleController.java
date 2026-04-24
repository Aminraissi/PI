package org.exemple.farmersupport.controller;

import lombok.RequiredArgsConstructor;
import org.exemple.farmersupport.entity.Parcelle;
import org.exemple.farmersupport.service.ParcelleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parcelles")
@RequiredArgsConstructor

public class ParcelleController {

    private final ParcelleService parcelleService;

    @PostMapping("/terrain/{terrainId}")
    public ResponseEntity<Parcelle> create(@PathVariable Long terrainId, @RequestBody Parcelle parcelle) {
        return new ResponseEntity<>(parcelleService.create(terrainId, parcelle), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Parcelle>> getAll() {
        return ResponseEntity.ok(parcelleService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Parcelle> getById(@PathVariable Long id) {
        return ResponseEntity.ok(parcelleService.getById(id));
    }

    @GetMapping("/terrain/{terrainId}")
    public ResponseEntity<List<Parcelle>> getByTerrain(@PathVariable Long terrainId) {
        return ResponseEntity.ok(parcelleService.getByTerrain(terrainId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Parcelle> update(@PathVariable Long id, @RequestBody Parcelle parcelle) {
        return ResponseEntity.ok(parcelleService.update(id, parcelle));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        parcelleService.delete(id);
        return ResponseEntity.ok("Parcelle deleted successfully");
    }
}