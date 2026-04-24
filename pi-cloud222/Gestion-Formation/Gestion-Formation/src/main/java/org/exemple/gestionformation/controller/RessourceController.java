package org.exemple.gestionformation.controller;


import lombok.RequiredArgsConstructor;
import org.exemple.gestionformation.entity.Ressource;
import org.exemple.gestionformation.service.RessourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RessourceController {

    private final RessourceService ressourceService;

    @PostMapping("/formations/{formationId}/ressources")
    @ResponseStatus(HttpStatus.CREATED)
    public Ressource create(@PathVariable Long formationId, @RequestBody Ressource ressource) {
        return ressourceService.create(formationId, ressource);
    }

    @PostMapping("/modules/{moduleId}/ressources")
    @ResponseStatus(HttpStatus.CREATED)
    public Ressource createForModule(@PathVariable Long moduleId, @RequestBody Ressource ressource) {
        return ressourceService.createForModule(moduleId, ressource);
    }

    @PostMapping("/formations/{formationId}/modules/{moduleId}/ressources")
    @ResponseStatus(HttpStatus.CREATED)
    public Ressource createForFormationModule(@PathVariable Long formationId, @PathVariable Long moduleId, @RequestBody Ressource ressource) {
        return ressourceService.createForModule(moduleId, ressource);
    }

    @GetMapping("/ressources")
    public List<Ressource> getAll() {
        return ressourceService.getAll();
    }

    @GetMapping("/ressources/{id}")
    public Ressource getById(@PathVariable Long id) {
        return ressourceService.getById(id);
    }

    @GetMapping("/formations/{formationId}/ressources")
    public List<Ressource> getByFormation(@PathVariable Long formationId) {
        return ressourceService.getByFormation(formationId);
    }

    @GetMapping("/modules/{moduleId}/ressources")
    public List<Ressource> getByModule(@PathVariable Long moduleId) {
        return ressourceService.getByModule(moduleId);
    }

    @GetMapping("/formations/{formationId}/modules/{moduleId}/ressources")
    public List<Ressource> getByFormationModule(@PathVariable Long formationId, @PathVariable Long moduleId) {
        return ressourceService.getByModule(moduleId);
    }

    @PutMapping("/ressources/{id}")
    public Ressource update(@PathVariable Long id, @RequestBody Ressource ressource) {
        return ressourceService.update(id, ressource);
    }

    @PutMapping("/formations/{formationId}/modules/{moduleId}/ressources/{id}")
    public Ressource updateForFormationModule(@PathVariable Long formationId, @PathVariable Long moduleId, @PathVariable Long id, @RequestBody Ressource ressource) {
        return ressourceService.update(id, ressource);
    }

    @DeleteMapping("/ressources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ressourceService.delete(id);
    }

    @DeleteMapping("/formations/{formationId}/modules/{moduleId}/ressources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteForFormationModule(@PathVariable Long formationId, @PathVariable Long moduleId, @PathVariable Long id) {
        ressourceService.delete(id);
    }

    @PostMapping(value = "/ressources/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadResource(@RequestParam("file") MultipartFile file) {
        String resourceUrl = ressourceService.uploadResource(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("resourceUrl", resourceUrl));
    }

    @PostMapping(value = "/formations/{formationId}/modules/{moduleId}/ressources/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadResourceForFormationModule(@PathVariable Long formationId, @PathVariable Long moduleId, @RequestParam("file") MultipartFile file) {
        String resourceUrl = ressourceService.uploadResource(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("resourceUrl", resourceUrl));
    }
}
