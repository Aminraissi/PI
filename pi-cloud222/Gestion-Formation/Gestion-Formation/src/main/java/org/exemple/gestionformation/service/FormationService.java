package org.exemple.gestionformation.service;

import org.exemple.gestionformation.entity.Formation;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FormationService {
     Formation create(Formation formation);
     List<Formation> getAll();
     Formation getById(Long id);
     Formation update(Long id, Formation data);
     void delete(Long id);
     String uploadImage(MultipartFile file);
}