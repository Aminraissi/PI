package org.exemple.assistance_service.service;

import org.exemple.assistance_service.dto.ReponseIADTO;

import java.util.List;

public interface ReponseIAService {
    ReponseIADTO create(Long demandeId, ReponseIADTO dto);
    ReponseIADTO update(Long id, ReponseIADTO dto);
    ReponseIADTO getById(Long id);
    List<ReponseIADTO> getAll();
    void delete(Long id);
}