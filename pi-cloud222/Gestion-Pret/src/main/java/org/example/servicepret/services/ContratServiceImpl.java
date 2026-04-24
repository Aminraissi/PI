package org.example.servicepret.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.example.servicepret.DTO.ContratResponseDTO;
import org.example.servicepret.DTO.User;
import org.example.servicepret.entities.*;
import org.example.servicepret.feign.IUserClient;
import org.example.servicepret.repositories.ContratRepo;
import org.example.servicepret.repositories.DemandePretRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class ContratServiceImpl implements IContratService {

    private final ContratRepo contratRepo;
    private final DemandePretRepo demandeRepository;
    private final EmailService emailService;
    private final IUserClient userClient;

    public List<Contrat> retrieveContrats() {
        return contratRepo.findAll();
    }

    public Contrat updateContrat(Contrat contrat) {
        return contratRepo.save(contrat);
    }

    public Contrat addContrat(Contrat contrat) {
        return contratRepo.save(contrat);
    }

    public Contrat retrieveContrat(long idContrat) {
        return contratRepo.findById(idContrat).orElse(null);
    }

    @Override
    public ContratResponseDTO getContrat(Long id) {

        Contrat contrat = contratRepo.findByIdWithDetails(id);

        if (contrat == null) {
            throw new RuntimeException("Contrat not found");
        }

        DemandePret demande = contrat.getDemande();

        User user = null;
        try {
            user = userClient.getUser(demande.getAgriculteurId());
        } catch (Exception e) {
            System.err.println("USER SERVICE ERROR: " + e.getMessage());
        }

        ContratResponseDTO dto = new ContratResponseDTO();
        dto.setContrat(contrat);
        dto.setDemande(demande);
        dto.setAgriculteur(user);

        return dto;
    }
    @Override
    public Contrat getByDemandeId(Long demandeId) {
        return contratRepo.findByDemandeId(demandeId);
    }

    public void removeContrat(long idContrat) {
        contratRepo.deleteById(idContrat);
    }
    public Contrat RecupererParDemande(Long demandeId)
    {
        return contratRepo.findByDemandeId(demandeId);
    }

    @Override
    public Contrat generateFromDemande(Long demandeId) {

        System.out.println("Génération contrat pour demande: " + demandeId);

        DemandePret demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande not found"));

        //  Récupération du token depuis la requête
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String token = null;

        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            token = request.getHeader("Authorization");
        }

        System.out.println("TOKEN BACKEND: " + token);

        Contrat contrat = new Contrat();
        contrat.setDateCreation(LocalDate.now());
        contrat.setMontant(demande.getMontantDemande());
        contrat.setStatutContrat(StatutContrat.NON_SIGNE);
        contrat.setDemande(demande);


        Contrat saved = contratRepo.save(contrat);

        try {
            //  PASSAGE DU TOKEN À FEIGN
            User user = userClient.getUser(demande.getAgriculteurId());

            System.out.println("Utilisateur récupéré: " + user.getEmail());

            emailService.sendContractEmail(user.getEmail(), saved.getId());

        } catch (Exception e) {
            System.err.println("Erreur Feign: " + e.getMessage());
        }

        return saved;
    }

    @Override
    public Contrat signContrat(Long contratId, String signatureBase64) {

        Contrat contrat = contratRepo.findById(contratId)
                .orElseThrow(() -> new RuntimeException("Contrat not found"));

        contrat.setSignatureBase64(signatureBase64);

        contrat.setStatutContrat(StatutContrat.SIGNE);

        return contratRepo.save(contrat);
    }
}