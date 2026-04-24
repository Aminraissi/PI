package org.exemple.assistance_service.service;

import org.exemple.assistance_service.dto.DemandeAssistanceDTO;

import java.util.List;

public interface DemandeAssistanceService {
    DemandeAssistanceDTO create(DemandeAssistanceDTO dto);
    DemandeAssistanceDTO update(Long id, DemandeAssistanceDTO dto);
    DemandeAssistanceDTO getById(Long id);
    List<DemandeAssistanceDTO> getAll();
    List<DemandeAssistanceDTO> getByUserId(Long userId);
    void delete(Long id);
}