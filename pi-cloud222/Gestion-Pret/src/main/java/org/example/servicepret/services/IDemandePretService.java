package org.example.servicepret.services;



import org.example.servicepret.DTO.DemandePretDTO;
import org.example.servicepret.entities.DemandePret;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IDemandePretService {
    public List<DemandePret> retrieveAllDemandes();

    public DemandePret updateDemande(DemandePret demandePret);

    public DemandePret addDemande(DemandePret demandePret);

    public DemandePret retrieveDemande(long idDemandePret);

    public void removeDemande(long idDemandePret);

    public DemandePret addDocuments(long id, List<MultipartFile> files);

    public byte[] getDocument(Long userId,long demandeId,String filename,String data,String sig);


        public long countDemandesByService(Long serviceId);

    public List<DemandePret> getByServiceId(long serviceId);

    public List<String> getDocumentNames(long id);
    String generateSignedUrl(Long userId, long demandeId, String filename, long validityMillis);
    public DemandePret scorerDemande(Long id);
    public DemandePret refuserDemande(Long id, String motif);
    public DemandePretDTO toDTO(DemandePret d);
    public List<DemandePret> getDemandesByAgriculteur(Long agriculteurId);

}