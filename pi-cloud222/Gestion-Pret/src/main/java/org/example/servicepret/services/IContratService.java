package org.example.servicepret.services;

import org.example.servicepret.DTO.ContratResponseDTO;
import org.example.servicepret.entities.Contrat;

import java.util.List;

public interface IContratService {
    public List<Contrat> retrieveContrats();
    public Contrat updateContrat (Contrat contrat);
    public Contrat addContrat (Contrat contrat);
    public Contrat retrieveContrat (long idContrat);
    public void removeContrat(long idContrat);

    public Contrat generateFromDemande(Long demandeId);
    public Contrat RecupererParDemande(Long demandeId);
    public Contrat signContrat(Long contratId, String signatureBase64);
    public Contrat getByDemandeId(Long demandeId);
    public ContratResponseDTO getContrat(Long id);

    }
