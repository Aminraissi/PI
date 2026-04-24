package org.exemple.farmersupport.controller;

import lombok.RequiredArgsConstructor;
import org.exemple.farmersupport.entity.Terrain;
import org.exemple.farmersupport.service.TerrainService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terrains")
@RequiredArgsConstructor

public class TerrainController {

    private final TerrainService terrainService;

    @PostMapping
    public ResponseEntity<Terrain> create(@RequestBody Terrain terrain) {
        return new ResponseEntity<>(terrainService.create(terrain), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Terrain>> getAll() {
        return ResponseEntity.ok(terrainService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Terrain> getById(@PathVariable Long id) {
        return ResponseEntity.ok(terrainService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Terrain> update(@PathVariable Long id, @RequestBody Terrain terrain) {
        return ResponseEntity.ok(terrainService.update(id, terrain));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        terrainService.delete(id);
        return ResponseEntity.ok("Terrain deleted successfully");
    }
    @GetMapping("/hello")
    public String hello() {
        return "hello from farmer supprt Service";
    }
}