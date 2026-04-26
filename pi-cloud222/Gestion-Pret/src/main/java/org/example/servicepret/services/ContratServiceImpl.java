package org.example.servicepret.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@AllArgsConstructor
public class ContratServiceImpl implements IContratService {
    private final String CONTRAT_STORAGE_PATH = "uploads/contrats/";
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

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String token = null;

        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            token = request.getHeader("Authorization");
        }

        User agriculteur =null;
        try {
            agriculteur = userClient.getUser(demande.getAgriculteurId());
            System.out.println("Agriculteur récupéré: " + agriculteur.getNom() + " " + agriculteur.getPrenom());
        } catch (Exception e) {
            System.err.println("Erreur récupération agriculteur: " + e.getMessage());
            throw new RuntimeException("Impossible de récupérer les informations de l'agriculteur");
        }

        ServicePret service = demande.getService();

        String contenuContrat = generateContratContent(demande, agriculteur, service);

        Contrat contrat = new Contrat();
        contrat.setDateCreation(LocalDate.now());
        contrat.setMontant(demande.getMontantDemande());
        contrat.setStatutContrat(StatutContrat.NON_SIGNE);
        contrat.setValidationAdmin(ValidationAdminStatus.PENDING);
        contrat.setContenuContrat(contenuContrat);
        contrat.setDemande(demande);

        Contrat saved = contratRepo.save(contrat);

        try {
            emailService.sendContractEmail(agriculteur.getEmail(), saved.getId());
        } catch (Exception e) {
            System.err.println("Erreur envoi email: " + e.getMessage());
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
    @Override
    @Transactional
    public Contrat signContratWithPDF(Long contratId, String signatureBase64, MultipartFile pdfFile) {
        Contrat contrat = contratRepo.findById(contratId)
                .orElseThrow(() -> new RuntimeException("Contrat not found"));

        contrat.setSignatureBase64(signatureBase64);
        contrat.setStatutContrat(StatutContrat.SIGNE);

        if (pdfFile != null && !pdfFile.isEmpty()) {
            try {
                Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads", "contrats");

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    System.out.println("Created directory: " + uploadPath.toAbsolutePath());
                }

                String fileName = "contrat_" + contratId + "_" + System.currentTimeMillis() + ".pdf";
                Path filePath = uploadPath.resolve(fileName);

                pdfFile.transferTo(filePath.toFile());

                System.out.println("PDF saved at: " + filePath.toAbsolutePath());

              contrat.setContenuContrat(filePath.toAbsolutePath().toString());

            } catch (IOException e) {
                System.err.println("Error saving PDF: " + e.getMessage());
                throw new RuntimeException("Error saving PDF file: " + e.getMessage(), e);
            }
        }

        return contratRepo.save(contrat);
    }
    private String generateContratContent(DemandePret demande, User agriculteur, ServicePret service) {
        StringBuilder contract = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        double interestRate = getInterestRate(demande, service);
        double penaltyRate = (service != null && service.getTauxPenalite() != null)
                ? service.getTauxPenalite()
                : 5.0;

        contract.append("AGRICULTURAL LOAN AGREEMENT\n");
        contract.append("================================\n\n");
        contract.append("Date: ").append(LocalDate.now().format(formatter)).append("\n\n");

        contract.append("ARTICLE 1: PARTIES\n");
        contract.append("-------------------\n");
        contract.append("Between the financial institution [Your Institution],\n");
        contract.append("And the farmer:\n");
        contract.append("  - Full Name: ").append(agriculteur.getNom()).append(" ").append(agriculteur.getPrenom()).append("\n");
        contract.append("  - National ID (CIN): ").append(agriculteur.getCin() != null ? agriculteur.getCin() : "Not provided").append("\n");
        contract.append("  - Email: ").append(agriculteur.getEmail() != null ? agriculteur.getEmail() : "Not provided").append("\n\n");

        contract.append("Loan requested for: ").append(demande.getObjet() != null ? demande.getObjet() : "Agricultural purposes").append("\n\n");

        contract.append("ARTICLE 2: AMOUNT AND DURATION\n");
        contract.append("---------------------------\n");
        contract.append("Loan amount: ").append(demande.getMontantDemande()).append(" DT\n");
        contract.append("Duration: ").append(demande.getDureeMois()).append(" months\n\n");

        contract.append("ARTICLE 3: INTEREST RATE\n");
        contract.append("-------------------------\n");
        contract.append("Annual interest rate: ").append(interestRate).append("%\n");
        if (service != null) {
            contract.append("Service: ").append(service.getNom()).append("\n");
        }
        contract.append("\n");

        contract.append("ARTICLE 4: REPAYMENT\n");
        contract.append("------------------------\n");
        double totalAmount = demande.getMontantDemande() * (1 + (interestRate / 100) * (demande.getDureeMois() / 12.0));
        double monthlyPayment = totalAmount / demande.getDureeMois();
        contract.append("Total amount to repay: ").append(Math.round(totalAmount)).append(" DT\n");
        contract.append("Monthly payment: ").append(Math.round(monthlyPayment)).append(" DT\n");
        contract.append("Number of installments: ").append(demande.getDureeMois()).append(" months\n\n");

        contract.append("ARTICLE 5: CONDITIONS\n");
        contract.append("--------------------\n");
        contract.append("Penalty rate for late payment: ").append(penaltyRate).append("%\n");
        contract.append("The borrower agrees to repay according to the agreed schedule.\n");
        contract.append("In case of delay, penalties will be applied on the outstanding amount.\n\n");

        contract.append("ARTICLE 6: GOVERNING LAW\n");
        contract.append("--------------------\n");
        contract.append("This contract is governed by the laws of Tunisia.\n");
        contract.append("Any dispute shall be submitted to the competent courts.\n\n");

        contract.append("ARTICLE 7: SIGNATURE\n");
        contract.append("--------------------\n");
        contract.append("The borrower acknowledges having read this contract, understood all clauses,\n");
        contract.append("and accepts all conditions without reservation.\n\n");

        contract.append("Done at [City], on ").append(LocalDate.now().format(formatter)).append("\n\n");
        contract.append("Borrower's signature: ____________________\n");
        contract.append("\n");
        contract.append("(Signed electronically)\n");

        return contract.toString();
    }
    @Override
    public List<ContratResponseDTO> getContratsEnAttenteValidation() {
        List<Contrat> contrats = contratRepo.findByStatutContratAndValidationAdmin(
                StatutContrat.SIGNE, ValidationAdminStatus.PENDING
        );

        List<ContratResponseDTO> result = new ArrayList<>();
        for (Contrat contrat : contrats) {
            DemandePret demande = contrat.getDemande();
            User agriculteur = null;
            try {
                agriculteur = userClient.getUser(demande.getAgriculteurId());
            } catch (Exception e) {
                System.err.println("Erreur récupération agriculteur pour contrat " + contrat.getId() + ": " + e.getMessage());
                agriculteur = new User();
                agriculteur.setNom("Inconnu");
                agriculteur.setPrenom("");
                agriculteur.setEmail("");
                agriculteur.setCin("");
            }

            ContratResponseDTO dto = new ContratResponseDTO();
            dto.setContrat(contrat);
            dto.setDemande(demande);
            dto.setAgriculteur(agriculteur);
            result.add(dto);
        }
        return result;
    }

    private double getInterestRate(DemandePret demande, ServicePret service) {
        if (service != null && service.getTauxInteret() != null) {
            return service.getTauxInteret();
        }


        return calculateInterestRate(demande.getScoreSolvabilite());
    }

    private double calculateInterestRate(Integer scoreSolvabilite) {
        if (scoreSolvabilite == null) return 12;
        if (scoreSolvabilite >= 80) return 5;
        if (scoreSolvabilite >= 50) return 8;
        return 12;
    }


    @Override
    public Contrat validerContratParAdmin(Long contratId, boolean valide) {
        Contrat contrat = contratRepo.findById(contratId)
                .orElseThrow(() -> new RuntimeException("Contrat not found"));

        if (contrat.getStatutContrat() != StatutContrat.SIGNE) {
            throw new RuntimeException("Le contrat doit être signé avant validation");
        }

        contrat.setValidationAdmin(valide ? ValidationAdminStatus.APPROVED : ValidationAdminStatus.REJECTED);

        return contratRepo.save(contrat);
    }



    @Override
    public byte[] getContratPDF(Long contratId) {
        Contrat contrat = contratRepo.findById(contratId)
                .orElseThrow(() -> new RuntimeException("Contrat not found"));

        String pdfPath = contrat.getContenuContrat();
        if (pdfPath == null || pdfPath.isEmpty()) {
            throw new RuntimeException("PDF file not found for this contract");
        }

        try {
            Path path = Paths.get(pdfPath);
            if (!Files.exists(path)) {
                throw new RuntimeException("PDF file does not exist at: " + pdfPath);
            }

            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Error reading PDF file: " + e.getMessage(), e);
        }
    }
    @Override
    public List<ContratResponseDTO> getValidatedContracts() {
        List<Contrat> contrats = contratRepo.findByValidationAdmin(ValidationAdminStatus.APPROVED);

        List<ContratResponseDTO> result = new ArrayList<>();
        for (Contrat contrat : contrats) {
            DemandePret demande = contrat.getDemande();
            User agriculteur = null;
            try {
                agriculteur = userClient.getUser(demande.getAgriculteurId());
            } catch (Exception e) {
                agriculteur = new User();
                agriculteur.setNom("Inconnu");
                agriculteur.setPrenom("");
            }

            ContratResponseDTO dto = new ContratResponseDTO();
            dto.setContrat(contrat);
            dto.setDemande(demande);
            dto.setAgriculteur(agriculteur);
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<ContratResponseDTO> getRejectedContracts() {
        List<Contrat> contrats = contratRepo.findByValidationAdmin(ValidationAdminStatus.REJECTED);

        List<ContratResponseDTO> result = new ArrayList<>();
        for (Contrat contrat : contrats) {
            DemandePret demande = contrat.getDemande();
            User agriculteur = null;
            try {
                agriculteur = userClient.getUser(demande.getAgriculteurId());
            } catch (Exception e) {
                agriculteur = new User();
                agriculteur.setNom("Inconnu");
                agriculteur.setPrenom("");
            }

            ContratResponseDTO dto = new ContratResponseDTO();
            dto.setContrat(contrat);
            dto.setDemande(demande);
            dto.setAgriculteur(agriculteur);
            result.add(dto);
        }
        return result;
    }
    @Override
    public List<Contrat> getAllContractsWithDetails() {
        return contratRepo.findAllWithDetails();
    }

    @Override
    public List<Contrat> getContractsByStatus(StatutContrat statut) {
        return contratRepo.findByStatutContrat(statut);
    }
}