package org.exemple.gestionformation.serviceImpl;




import lombok.RequiredArgsConstructor;
import org.exemple.gestionformation.entity.Formation;
import org.exemple.gestionformation.entity.Module;
import org.exemple.gestionformation.repository.FormationRepository;
import org.exemple.gestionformation.repository.ModuleRepository;
import org.exemple.gestionformation.service.ModuleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final FormationRepository formationRepository;

    public Module create(Long formationId, Module module) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new RuntimeException("Formation not found with id: " + formationId));

        module.setFormation(formation);
        return moduleRepository.save(module);
    }

    public List<Module> getAll() {
        return moduleRepository.findAll();
    }

    public Module getById(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found with id: " + id));
    }

    public List<Module> getByFormation(Long formationId) {
        return moduleRepository.findByFormationIdFormation(formationId);
    }

    public Module update(Long id, Module newData) {
        Module module = getById(id);
        module.setTitre(newData.getTitre());
        module.setOrdre(newData.getOrdre());
        return moduleRepository.save(module);
    }

    public void delete(Long id) {
        Module module = getById(id);
        moduleRepository.delete(module);
    }
}
