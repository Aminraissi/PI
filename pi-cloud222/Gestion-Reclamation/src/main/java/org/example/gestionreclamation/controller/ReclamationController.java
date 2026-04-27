package org.example.gestionreclamation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestionreclamation.dto.*;
import org.example.gestionreclamation.enums.ReclamationStatus;
import org.example.gestionreclamation.service.ReclamationAiService;
import org.example.gestionreclamation.service.ReclamationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reclamations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ReclamationController {

    private final ReclamationService reclamationService;
    private final ReclamationAiService reclamationAiService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ReclamationResponse create(@Valid @RequestBody CreateReclamationRequest request) {
        return reclamationService.create(request);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReclamationResponse createWithAttachment(
            @Valid @RequestPart("data") CreateReclamationRequest request,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
        return reclamationService.create(request, attachment);
    }

    @GetMapping
    public List<ReclamationResponse> getAll(@RequestParam(required = false) ReclamationStatus status) {
        return reclamationService.getAll(status);
    }

    @GetMapping("/user/{userId}")
    public List<ReclamationResponse> getByUser(@PathVariable Long userId) {
        return reclamationService.getByUser(userId);
    }

    @GetMapping("/{id}")
    public ReclamationResponse getById(@PathVariable Long id) {
        return reclamationService.getById(id);
    }

    @PostMapping("/{id}/messages")
    public ReclamationResponse addMessage(@PathVariable Long id, @Valid @RequestBody AddMessageRequest request) {
        return reclamationService.addMessage(id, request);
    }

    @PatchMapping("/{id}/status")
    public ReclamationResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return reclamationService.updateStatus(id, request);
    }

    @PostMapping("/ai/correct-description")
    public CorrectDescriptionResponse correctDescription(@RequestBody CorrectDescriptionRequest request) {
        return new CorrectDescriptionResponse(reclamationAiService.correctDescription(request));
    }
}
