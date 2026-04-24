package org.exemple.paymentservice.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemple.paymentservice.dtos.FactureDTO;
import org.exemple.paymentservice.entities.Facture;
import org.exemple.paymentservice.repositories.FactureRepository;
import org.exemple.paymentservice.services.FactureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Implementation of FactureService
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FactureServiceImpl implements FactureService {

    private final FactureRepository factureRepository;

    @Override
    public Facture createFactureAuto(Double montant) {
        log.info("Creating auto-generated Facture with amount: {}", montant);

        // Generate unique facture number: FAC-TIMESTAMP-RANDOMUUID
        String numero = generateFactureNumero();

        Facture facture = Facture.builder()
                .numero(numero)
                .date(LocalDate.now())
                .total(montant)
                .pdfUrl(null)  // Can be set later
                .build();

        Facture saved = factureRepository.save(facture);
        log.info("Facture created successfully with numero: {}", numero);

        return saved;
    }

    @Override
    public FactureDTO convertToDTO(Facture facture) {
        if (facture == null) {
            return null;
        }
        return FactureDTO.builder()
                .idFacture(facture.getIdFacture())
                .numero(facture.getNumero())
                .date(facture.getDate())
                .total(facture.getTotal())
                .pdfUrl(facture.getPdfUrl())
                .build();
    }

    @Override
    public Facture convertToEntity(FactureDTO factureDTO) {
        if (factureDTO == null) {
            return null;
        }
        return Facture.builder()
                .idFacture(factureDTO.getIdFacture())
                .numero(factureDTO.getNumero())
                .date(factureDTO.getDate())
                .total(factureDTO.getTotal())
                .pdfUrl(factureDTO.getPdfUrl())
                .build();
    }

    /**
     * Generate a unique facture number with pattern: FAC-YYYYMMDDHHMMSS-RANDOMUUID(8)
     * Example: FAC-20240316101530-A7F2E9C1
     */
    private String generateFactureNumero() {
        long timestamp = System.currentTimeMillis();
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "FAC-" + timestamp + "-" + randomPart;
    }
}

