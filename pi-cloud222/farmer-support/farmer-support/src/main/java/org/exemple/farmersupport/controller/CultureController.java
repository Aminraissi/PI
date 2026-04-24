package org.exemple.farmersupport.controller;

import lombok.RequiredArgsConstructor;
import org.exemple.farmersupport.entity.Culture;
import org.exemple.farmersupport.service.CultureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cultures")
@RequiredArgsConstructor

public class CultureController {

    private final CultureService cultureService;

    @PostMapping("/parcelle/{parcelleId}")
    public ResponseEntity<Culture> create(@PathVariable Long parcelleId, @RequestBody Culture culture) {
        return new ResponseEntity<>(cultureService.create(parcelleId, culture), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Culture>> getAll() {
        return ResponseEntity.ok(cultureService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Culture> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cultureService.getById(id));
    }

    @GetMapping("/parcelle/{parcelleId}")
    public ResponseEntity<List<Culture>> getByParcelle(@PathVariable Long parcelleId) {
        return ResponseEntity.ok(cultureService.getByParcelle(parcelleId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Culture> update(@PathVariable Long id, @RequestBody Culture culture) {
        return ResponseEntity.ok(cultureService.update(id, culture));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        cultureService.delete(id);
        return ResponseEntity.ok("Culture deleted successfully");
    }
}