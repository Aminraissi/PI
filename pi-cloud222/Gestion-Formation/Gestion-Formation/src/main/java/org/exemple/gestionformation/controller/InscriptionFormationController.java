package org.exemple.gestionformation.controller;


import lombok.RequiredArgsConstructor;
import org.exemple.gestionformation.entity.InscriptionFormation;
import org.exemple.gestionformation.service.InscriptionFormationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InscriptionFormationController {

    private final InscriptionFormationService inscriptionFormationService;

    @PostMapping("/formations/{formationId}/inscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public InscriptionFormation create(@PathVariable Long formationId,
                                       @RequestBody InscriptionFormation inscription) {
        return inscriptionFormationService.create(formationId, inscription);
    }

    @GetMapping("/inscriptions")
    public List<InscriptionFormation> getAll() {
        return inscriptionFormationService.getAll();
    }

    @GetMapping("/inscriptions/{id}")
    public InscriptionFormation getById(@PathVariable Long id) {
        return inscriptionFormationService.getById(id);
    }

    @GetMapping("/formations/{formationId}/inscriptions")
    public List<InscriptionFormation> getByFormation(@PathVariable Long formationId) {
        return inscriptionFormationService.getByFormation(formationId);
    }

    @GetMapping("/formations/user/{userId}/inscriptions")
    public List<InscriptionFormation> getByUser(@PathVariable Long userId) {
        return inscriptionFormationService.getByUser(userId);
    }

    @PutMapping("/inscriptions/{id}")
    public InscriptionFormation update(@PathVariable Long id,
                                       @RequestBody InscriptionFormation inscription) {
        return inscriptionFormationService.update(id, inscription);
    }

    @DeleteMapping("/inscriptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        inscriptionFormationService.delete(id);
    }
}
