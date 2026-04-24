package org.exemple.farmersupport.controller;

import lombok.RequiredArgsConstructor;
import org.exemple.farmersupport.entity.Rappel;
import org.exemple.farmersupport.service.RappelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rappels")
@RequiredArgsConstructor

public class RappelController {

    private final RappelService rappelService;

    @PostMapping("/event/{eventId}")
    public ResponseEntity<Rappel> create(@PathVariable Long eventId, @RequestBody Rappel rappel) {
        return new ResponseEntity<>(rappelService.create(eventId, rappel), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Rappel>> getAll() {
        return ResponseEntity.ok(rappelService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rappel> getById(@PathVariable Long id) {
        return ResponseEntity.ok(rappelService.getById(id));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Rappel>> getByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(rappelService.getByEvent(eventId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Rappel> update(@PathVariable Long id, @RequestBody Rappel rappel) {
        return ResponseEntity.ok(rappelService.update(id, rappel));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        rappelService.delete(id);
        return ResponseEntity.ok("Rappel deleted successfully");
    }
}