package org.exemple.assistance_service.service;

import org.exemple.assistance_service.dto.ReponseIngenieurDTO;

import java.util.List;

public interface ReponseIngenieurService {
    ReponseIngenieurDTO create(Long affectationId, ReponseIngenieurDTO dto);
    ReponseIngenieurDTO update(Long id, ReponseIngenieurDTO dto);
    ReponseIngenieurDTO getById(Long id);
    List<ReponseIngenieurDTO> getAll();
    void delete(Long id);
}