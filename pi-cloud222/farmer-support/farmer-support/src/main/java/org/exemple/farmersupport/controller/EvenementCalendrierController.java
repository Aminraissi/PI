package org.exemple.farmersupport.controller;

import lombok.RequiredArgsConstructor;
import org.exemple.farmersupport.entity.EvenementCalendrier;
import org.exemple.farmersupport.service.EvenementCalendrierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evenements")
@RequiredArgsConstructor

public class EvenementCalendrierController {

    private final EvenementCalendrierService evenementService;

    @PostMapping
    public ResponseEntity<EvenementCalendrier> create(@RequestBody EvenementCalendrier evenement) {
        return new ResponseEntity<>(evenementService.create(evenement), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EvenementCalendrier>> getAll() {
        return ResponseEntity.ok(evenementService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvenementCalendrier> getById(@PathVariable Long id) {
        return ResponseEntity.ok(evenementService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvenementCalendrier> update(@PathVariable Long id, @RequestBody EvenementCalendrier evenement) {
        return ResponseEntity.ok(evenementService.update(id, evenement));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        evenementService.delete(id);
        return ResponseEntity.ok("Evenement deleted successfully");
    }
}