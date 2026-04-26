package org.example.servicepret.services;

import org.example.servicepret.DTO.ContratResponseDTO;
import org.example.servicepret.entities.Contrat;
import org.example.servicepret.entities.StatutContrat;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IContratService {
    public List<Contrat> retrieveContrats();
    public Contrat updateContrat (Contrat contrat);
    public Contrat addContrat (Contrat contrat);
    public Contrat retrieveContrat (long idContrat);
    public void removeContrat(long idContrat);
    public Contrat signContrat(Long contratId, String signatureBase64);
    public Contrat generateFromDemande(Long demandeId);
    public Contrat RecupererParDemande(Long demandeId);
    public Contrat signContratWithPDF(Long contratId, String signatureBase64, MultipartFile pdfFile);
    public Contrat getByDemandeId(Long demandeId);
    public ContratResponseDTO getContrat(Long id);
    public List<ContratResponseDTO> getContratsEnAttenteValidation();

    public Contrat validerContratParAdmin(Long contratId, boolean valide);
    public byte[] getContratPDF(Long contratId);
    public List<ContratResponseDTO> getValidatedContracts();
    public List<ContratResponseDTO> getRejectedContracts();
    public List<Contrat> getAllContractsWithDetails();
    public List<Contrat> getContractsByStatus(StatutContrat statut);
}