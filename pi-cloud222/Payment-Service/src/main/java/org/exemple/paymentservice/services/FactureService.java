package org.exemple.paymentservice.services;

import org.exemple.paymentservice.dtos.FactureDTO;
import org.exemple.paymentservice.entities.Facture;

/**
 * Service interface for Facture operations
 */
public interface FactureService {

    /**
     * Create a new Facture with auto-generated numero and date
     * @param montant the total amount for the facture
     * @return created FactureDTO
     */
    Facture createFactureAuto(Double montant);

    /**
     * Convert Facture entity to FactureDTO
     * @param facture Facture entity
     * @return FactureDTO
     */
    FactureDTO convertToDTO(Facture facture);

    /**
     * Convert FactureDTO to Facture entity
     * @param factureDTO FactureDTO
     * @return Facture entity
     */
    Facture convertToEntity(FactureDTO factureDTO);
    byte[] generateFacturePdf(Long factureId);
}

