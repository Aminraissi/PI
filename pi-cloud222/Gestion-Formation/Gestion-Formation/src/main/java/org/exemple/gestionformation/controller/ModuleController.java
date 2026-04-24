package org.exemple.gestionformation.controller;


import lombok.RequiredArgsConstructor;
import org.exemple.gestionformation.entity.Module;
import org.exemple.gestionformation.service.ModuleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @PostMapping("/formations/{formationId}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public Module create(@PathVariable Long formationId, @RequestBody Module module) {
        return moduleService.create(formationId, module);
    }

    @GetMapping("/modules")
    public List<Module> getAll() {
        return moduleService.getAll();
    }

    @GetMapping("/modules/{id}")
    public Module getById(@PathVariable Long id) {
        return moduleService.getById(id);
    }

    @GetMapping("/formations/{formationId}/modules")
    public List<Module> getByFormation(@PathVariable Long formationId) {
        return moduleService.getByFormation(formationId);
    }

    @PutMapping("/modules/{id}")
    public Module update(@PathVariable Long id, @RequestBody Module module) {
        return moduleService.update(id, module);
    }

    @DeleteMapping("/modules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        moduleService.delete(id);
    }
}