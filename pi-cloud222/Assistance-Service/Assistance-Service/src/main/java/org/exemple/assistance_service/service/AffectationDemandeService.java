package org.exemple.assistance_service.service;

import org.exemple.assistance_service.dto.AffectationDemandeDTO;

import java.util.List;

public interface AffectationDemandeService {
    AffectationDemandeDTO create(Long demandeId, AffectationDemandeDTO dto);
    AffectationDemandeDTO update(Long id, AffectationDemandeDTO dto);
    AffectationDemandeDTO getById(Long id);
    List<AffectationDemandeDTO> getAll();
    List<AffectationDemandeDTO> getByIngenieurId(Long ingenieurId);
    List<AffectationDemandeDTO> getPendingByIngenieurId(Long ingenieurId);
    AffectationDemandeDTO accept(Long id, Long ingenieurId);
    AffectationDemandeDTO refuse(Long id, Long ingenieurId);
    void delete(Long id);
}
