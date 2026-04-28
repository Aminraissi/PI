package org.example.gestionreclamation.service;

import org.example.gestionreclamation.dto.*;
import org.example.gestionreclamation.enums.ReclamationStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReclamationService {
    ReclamationResponse create(CreateReclamationRequest request);
    ReclamationResponse create(CreateReclamationRequest request, MultipartFile attachment);
    List<ReclamationResponse> getAll(ReclamationStatus status);
    List<ReclamationResponse> getByUser(Long userId);
    ReclamationResponse getById(Long id);
    ReclamationResponse addMessage(Long reclamationId, AddMessageRequest request);
    ReclamationResponse updateStatus(Long reclamationId, UpdateStatusRequest request);
}
