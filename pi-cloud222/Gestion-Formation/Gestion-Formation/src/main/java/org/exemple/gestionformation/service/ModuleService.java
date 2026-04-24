package org.exemple.gestionformation.service;

import org.exemple.gestionformation.entity.Module;
import java.util.List;


public interface ModuleService {
    Module create(Long formationId, Module module);
    List<Module> getAll();
    Module getById(Long id);
    List<Module> getByFormation(Long formationId);
    Module update(Long id, Module newData);
    void delete(Long id);
}
