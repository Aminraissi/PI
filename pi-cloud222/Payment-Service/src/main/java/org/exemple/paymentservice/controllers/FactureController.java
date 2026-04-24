package org.exemple.paymentservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemple.paymentservice.dtos.FactureDTO;
import org.exemple.paymentservice.entities.Facture;
import org.exemple.paymentservice.repositories.FactureRepository;
import org.exemple.paymentservice.services.FactureService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
@Validated
@Slf4j
public class FactureController {

    private final FactureRepository factureRepository;

    private final FactureService factureService;

    @GetMapping
    public ResponseEntity<List<FactureDTO>> getAllFactures() {
        log.info("GET /api/factures - Get all factures");
        List<FactureDTO> factures = factureRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(factures);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FactureDTO> getFactureById(
            @PathVariable  Long id) {
        log.info("GET /api/factures/{} - Get facture by id", id);
        return factureRepository.findById(id)
                .map(facture -> ResponseEntity.ok(convertToDTO(facture)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping("/numero/{numero}")
    public ResponseEntity<FactureDTO> getFactureByNumero(
            @PathVariable String numero) {
        log.info("GET /api/factures/numero/{} - Get facture by numero", numero);
        return factureRepository.findByNumero(numero)
                .map(facture -> ResponseEntity.ok(convertToDTO(facture)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @PostMapping
    public ResponseEntity<FactureDTO> createFacture( @RequestBody FactureDTO factureDTO) {
        log.info("POST /api/factures - Create facture");
        Facture facture = convertToEntity(factureDTO);
        Facture saved = factureRepository.save(facture);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
    }


    @PutMapping("/{id}")
    public ResponseEntity<FactureDTO> updateFacture(
            @PathVariable  Long id,
            @RequestBody FactureDTO factureDTO) {
        log.info("PUT /api/factures/{} - Update facture", id);
        return factureRepository.findById(id)
                .map(facture -> {
                    if (factureDTO.getNumero() != null) {
                        facture.setNumero(factureDTO.getNumero());
                    }
                    if (factureDTO.getDate() != null) {
                        facture.setDate(factureDTO.getDate());
                    }
                    if (factureDTO.getTotal() != null) {
                        facture.setTotal(factureDTO.getTotal());
                    }
                    if (factureDTO.getPdfUrl() != null) {
                        facture.setPdfUrl(factureDTO.getPdfUrl());
                    }
                    Facture updated = factureRepository.save(facture);
                    return ResponseEntity.ok(convertToDTO(updated));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFacture(
            @PathVariable  Long id) {
        log.info("DELETE /api/factures/{} - Delete facture", id);
        if (factureRepository.existsById(id)) {
            factureRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }


    private FactureDTO convertToDTO(Facture facture) {
        return FactureDTO.builder()
                .idFacture(facture.getIdFacture())
                .numero(facture.getNumero())
                .date(facture.getDate())
                .total(facture.getTotal())
                .pdfUrl(facture.getPdfUrl())
                .build();
    }


    private Facture convertToEntity(FactureDTO factureDTO) {
        return Facture.builder()
                .numero(factureDTO.getNumero())
                .date(factureDTO.getDate())
                .total(factureDTO.getTotal())
                .pdfUrl(factureDTO.getPdfUrl())
                .build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generateFacturePdf(@PathVariable Long id) {
        byte[] pdf = factureService.generateFacturePdf(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=facture-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

