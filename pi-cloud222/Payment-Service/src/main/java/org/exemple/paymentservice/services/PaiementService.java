package org.exemple.paymentservice.services;

import org.exemple.paymentservice.dtos.PaiementDTO;

import java.util.List;


public interface PaiementService {
    List<PaiementDTO> getAllPaiements();
    PaiementDTO getPaiementById(Long idPaiement);
    PaiementDTO getPaiementByReference(String reference);
    PaiementDTO getPaiementByFactureId(Long factureId);
    List<PaiementDTO> getPaiementsByFactureId(Long factureId);
    PaiementDTO createPaiement(Long factureId, PaiementDTO paiementDTO);
    PaiementDTO createPaiement(PaiementDTO paiementDTO);
    PaiementDTO updatePaiement(Long idPaiement, PaiementDTO paiementDTO);
    void deletePaiement(Long idPaiement);
    List<PaiementDTO> getPaidPaiementsByUser(Long userId);
}

