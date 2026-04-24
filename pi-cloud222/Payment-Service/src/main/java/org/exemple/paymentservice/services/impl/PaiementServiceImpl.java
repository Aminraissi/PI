package org.exemple.paymentservice.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemple.paymentservice.dtos.PaiementDTO;
import org.exemple.paymentservice.entities.Facture;
import org.exemple.paymentservice.entities.Paiement;
import org.exemple.paymentservice.enums.StatutPaiement;
import org.exemple.paymentservice.exceptions.FactureNotFoundException;
import org.exemple.paymentservice.exceptions.PaiementAlreadyExistsException;
import org.exemple.paymentservice.exceptions.PaiementNotFoundException;
import org.exemple.paymentservice.repositories.FactureRepository;
import org.exemple.paymentservice.repositories.PaiementRepository;
import org.exemple.paymentservice.services.PaiementService;
import org.exemple.paymentservice.services.FactureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaiementServiceImpl implements PaiementService {

    private final PaiementRepository paiementRepository;
    private final FactureRepository factureRepository;
    private final FactureService factureService;

    @Override
    @Transactional(readOnly = true)
    public List<PaiementDTO> getAllPaiements() {
        log.info("Fetching all payments");
        return paiementRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaiementDTO getPaiementById(Long idPaiement) {
        log.info("Fetching payment with id: {}", idPaiement);
        return paiementRepository.findById(idPaiement)
                .map(this::convertToDTO)
                .orElseThrow(() -> new PaiementNotFoundException(idPaiement));
    }

    @Override
    @Transactional(readOnly = true)
    public PaiementDTO getPaiementByReference(String reference) {
        log.info("Fetching payment with reference: {}", reference);
        return paiementRepository.findByReference(reference)
                .map(this::convertToDTO)
                .orElseThrow(() -> new PaiementNotFoundException("Payment not found with reference: " + reference));
    }

    @Override
    @Transactional(readOnly = true)
    public PaiementDTO getPaiementByFactureId(Long factureId) {
        log.info("Fetching payment for facture with id: {}", factureId);
        return paiementRepository.findByFactureIdFacture(factureId)
                .map(this::convertToDTO)
                .orElseThrow(() -> new PaiementNotFoundException("Payment not found for facture with id: " + factureId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementDTO> getPaiementsByFactureId(Long factureId) {
        log.info("Fetching all payments for facture with id: {}", factureId);
        // Verify that the facture exists
        if (!factureRepository.existsById(factureId)) {
            throw new FactureNotFoundException(factureId);
        }
        return paiementRepository.findByFactureIdFactureOrderByDatePaiementDesc(factureId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PaiementDTO createPaiement(Long factureId, PaiementDTO paiementDTO) {
        log.info("Creating payment for facture with id: {}", factureId);

        // Verify that the facture exists
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new FactureNotFoundException(factureId));

        // Check if a payment already exists for this facture
        if (facture.getPaiement() != null) {
            throw new PaiementAlreadyExistsException("A payment already exists for facture with id: " + factureId);
        }

        Paiement paiement = convertToEntity(paiementDTO);
        paiement.setFacture(facture);
        
        // Generate payment reference if not provided
        if (paiement.getReference() == null || paiement.getReference().isEmpty()) {
            paiement.setReference(generatePaymentReference());
        }

        Paiement savedPaiement = paiementRepository.save(paiement);
        log.info("Payment created successfully with id: {} and reference: {}", 
                savedPaiement.getIdPaiement(), savedPaiement.getReference());

        return convertToDTO(savedPaiement);
    }

    @Override
    public PaiementDTO createPaiement(PaiementDTO paiementDTO) {
        log.info("Creating payment with automatic facture generation");

        // Validate required fields for auto-generation
        if (paiementDTO.getMontant() == null || paiementDTO.getMontant() <= 0) {
            throw new IllegalArgumentException("Montant must be greater than 0");
        }
        if (paiementDTO.getMethode() == null) {
            throw new IllegalArgumentException("Methode is required");
        }
        if (paiementDTO.getStatut() == null) {
            throw new IllegalArgumentException("Statut is required");
        }

        // Auto-create facture
        Facture autoFacure = factureService.createFactureAuto(paiementDTO.getMontant());
        log.info("Auto-generated Facture with numero: {}", autoFacure.getNumero());

        // Create payment with auto-generated facture
        Paiement paiement = convertToEntity(paiementDTO);
        paiement.setFacture(autoFacure);
        
        // Set date if not provided
        if (paiement.getDatePaiement() == null) {
            paiement.setDatePaiement(LocalDateTime.now());
        }
        
        // Generate payment reference
        paiement.setReference(generatePaymentReference());

        Paiement savedPaiement = paiementRepository.save(paiement);
        log.info("Payment created with id: {}, reference: {}, and linked to Facture: {}", 
                savedPaiement.getIdPaiement(), savedPaiement.getReference(), autoFacure.getNumero());

        return convertToDTO(savedPaiement);
    }

    @Override
    public PaiementDTO updatePaiement(Long idPaiement, PaiementDTO paiementDTO) {
        log.info("Updating payment with id: {}", idPaiement);

        Paiement paiement = paiementRepository.findById(idPaiement)
                .orElseThrow(() -> new PaiementNotFoundException(idPaiement));

        if (paiementDTO.getMontant() != null) {
            paiement.setMontant(paiementDTO.getMontant());
        }
        if (paiementDTO.getDatePaiement() != null) {
            paiement.setDatePaiement(paiementDTO.getDatePaiement());
        }
        if (paiementDTO.getMethode() != null) {
            paiement.setMethode(paiementDTO.getMethode());
        }
        if (paiementDTO.getStatut() != null) {
            paiement.setStatut(paiementDTO.getStatut());
        }
        if (paiementDTO.getReference() != null) {
            paiement.setReference(paiementDTO.getReference());
        }

        Paiement updatedPaiement = paiementRepository.save(paiement);
        log.info("Payment updated successfully with id: {}", updatedPaiement.getIdPaiement());

        return convertToDTO(updatedPaiement);
    }

    @Override
    public void deletePaiement(Long idPaiement) {
        log.info("Deleting payment with id: {}", idPaiement);

        if (!paiementRepository.existsById(idPaiement)) {
            throw new PaiementNotFoundException(idPaiement);
        }

        paiementRepository.deleteById(idPaiement);
        log.info("Payment deleted successfully with id: {}", idPaiement);
    }


    private PaiementDTO convertToDTO(Paiement paiement) {
        if (paiement == null) {
            return null;
        }
        
        PaiementDTO dto = PaiementDTO.builder()
                .idPaiement(paiement.getIdPaiement())
                .montant(paiement.getMontant())
                .datePaiement(paiement.getDatePaiement())
                .methode(paiement.getMethode())
                .statut(paiement.getStatut())
                .reference(paiement.getReference())
                .userId(paiement.getUserId())
                .commandeId(paiement.getCommandeId())
                .build();
        
        // Include facture details in response
        if (paiement.getFacture() != null) {
            dto.setFactureId(paiement.getFacture().getIdFacture());
            dto.setFacture(factureService.convertToDTO(paiement.getFacture()));
        }
        
        return dto;
    }


    private Paiement convertToEntity(PaiementDTO paiementDTO) {
        return Paiement.builder()
                .montant(paiementDTO.getMontant())
                .datePaiement(paiementDTO.getDatePaiement())
                .methode(paiementDTO.getMethode())
                .statut(paiementDTO.getStatut())
                .reference(paiementDTO.getReference())
                .userId(paiementDTO.getUserId())
                .commandeId(paiementDTO.getCommandeId())
                .build();
    }

    /**
     * Generate a unique payment reference with pattern: PAY-TIMESTAMP-RANDOMUUID(8)
     * Example: PAY-1710589530000-A7F2E9C1
     */
    private String generatePaymentReference() {
        long timestamp = System.currentTimeMillis();
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PAY-" + timestamp + "-" + randomPart;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementDTO> getPaidPaiementsByUser(Long userId) {
        log.info("Fetching paid payments for user: {}", userId);

        return paiementRepository
                .findByUserIdAndStatutOrderByDatePaiementDesc(userId, StatutPaiement.PAYE)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}

