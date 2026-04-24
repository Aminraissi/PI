package org.exemple.gestionformation.service;

import org.exemple.gestionformation.entity.InscriptionFormation;
import java.util.List;

public interface InscriptionFormationService {
     InscriptionFormation create(Long formationId, InscriptionFormation inscription);
     List<InscriptionFormation> getAll();
     InscriptionFormation getById(Long id);
     List<InscriptionFormation> getByFormation(Long formationId);
     List<InscriptionFormation> getByUser(Long userId);
     InscriptionFormation update(Long id, InscriptionFormation newData);
     void delete(Long id);
}