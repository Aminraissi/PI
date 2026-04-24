package org.exemple.gestionformation.service;

import org.exemple.gestionformation.entity.Ressource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface RessourceService {

    Ressource create(Long formationId, Ressource ressource);
    Ressource createForModule(Long moduleId, Ressource ressource);
    List<Ressource> getAll();
    Ressource getById(Long id);
    List<Ressource> getByFormation(Long formationId);
    List<Ressource> getByModule(Long moduleId);
    Ressource update(Long id, Ressource newData);
    void delete(Long id);
    String uploadResource(MultipartFile file);
}
