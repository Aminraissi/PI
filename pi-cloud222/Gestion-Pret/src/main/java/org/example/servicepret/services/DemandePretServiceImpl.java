package org.example.servicepret.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.servicepret.DTO.DemandePretDTO;
import org.example.servicepret.DTO.FraudAnalysisResult;
import org.example.servicepret.DTO.User;
import org.example.servicepret.entities.*;
import org.example.servicepret.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.example.servicepret.feign.IUserClient;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class DemandePretServiceImpl implements IDemandePretService {

    private static final String UPLOAD_DIR = "uploads/demandes/";

    private final DemandePretRepo demandePretRepo;
    private final DemandePretDocumentRepo documentRepo;
    private final ServiceRepo servicePretRepo;
    private final DocumentAccessLogRepo logRepo;
    private final ScoringClientService scoringClientService;
    private final IUserClient userClient;
    private final FraudDetectionService fraudDetectionService;

    @Value("${crypto.master.secret}")
    private String masterSecret;

    public DemandePretServiceImpl(
            DemandePretRepo demandePretRepo,
            DemandePretDocumentRepo documentRepo,
            DocumentAccessLogRepo logRepo,
            ServiceRepo servicePretRepo,
            ScoringClientService scoringClientService,
            IUserClient userClient,
            FraudDetectionService fraudDetectionService
    ) {
        this.demandePretRepo = demandePretRepo;
        this.documentRepo = documentRepo;
        this.logRepo = logRepo;
        this.servicePretRepo = servicePretRepo;
        this.scoringClientService = scoringClientService;
        this.userClient = userClient;
        this.fraudDetectionService = fraudDetectionService;
    }



    private byte[] getMasterKey() {
        if (masterSecret == null || masterSecret.isEmpty()) {
            throw new RuntimeException("MASTER SECRET NOT CONFIGURED");
        }
        return Base64.getDecoder().decode(masterSecret);
    }

    private SecretKey getHmacKey() {
        byte[] key = getMasterKey();


        byte[] hmacKey = new byte[32];
        System.arraycopy(key, 0, hmacKey, 0, Math.min(key.length, 32));

        return new SecretKeySpec(hmacKey, "HmacSHA256");
    }


    private String generateDataKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        return Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
    }

    private String encryptDataKey(String dataKey) throws Exception {
        byte[] encrypted = CryptoService.encrypt(dataKey.getBytes(), Base64.getEncoder().encodeToString(getMasterKey()));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decryptDataKey(String encryptedKey) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedKey);
        byte[] decrypted = CryptoService.decrypt(decoded, Base64.getEncoder().encodeToString(getMasterKey()));
        return new String(decrypted);
    }


    @Override
    public List<DemandePret> retrieveAllDemandes() {
        return demandePretRepo.findAll();
    }

    @Override
    public DemandePret retrieveDemande(long id) {
        return demandePretRepo.findById(id).orElse(null);
    }

    @Override
    public void removeDemande(long id) {
        demandePretRepo.deleteById(id);
    }

    @Override
    public DemandePret updateDemande(DemandePret demandePret) {
        return demandePretRepo.save(demandePret);
    }

    @Override
    public DemandePret addDemande(DemandePret demandePret) {

        try {
            ServicePret service = servicePretRepo.findById(
                    demandePret.getService().getId()
            ).orElseThrow(() -> new RuntimeException("Service not found"));

            demandePret.setService(service);

            String dataKey = generateDataKey();
            String encryptedKey = encryptDataKey(dataKey);
            demandePret.setEncryptedDataKey(encryptedKey);

            // 1. SAVE
            DemandePret saved = demandePretRepo.save(demandePret);


            return saved;

        } catch (Exception e) {
            throw new RuntimeException("ADD DEMANDE ERROR", e);
        }
    }


    @Override
    public DemandePret addDocuments(long id, List<MultipartFile> files) {
        System.out.println("🚨🚨🚨 [FRAUD] addDocuments CALLED for id=" + id + " with " + files.size() + " files 🚨🚨🚨");
        DemandePret demande = demandePretRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande not found"));

        try {
            if (demande.getEncryptedDataKey() == null) {
                throw new RuntimeException("Data key NOT initialized");
            }

            String dataKey = decryptDataKey(demande.getEncryptedDataKey());

            Path uploadPath = Paths.get(UPLOAD_DIR, String.valueOf(id));
            Files.createDirectories(uploadPath);

            for (MultipartFile file : files) {

                String originalName = file.getOriginalFilename();

                String safeFileName = System.currentTimeMillis() + "_" +
                        (originalName != null ? originalName : "file")
                                .replaceAll("[^a-zA-Z0-9\\.]", "_");

                byte[] originalBytes = file.getBytes();

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(originalBytes);
                String hash = Base64.getEncoder().encodeToString(hashBytes);

                byte[] encrypted = CryptoService.encrypt(originalBytes, dataKey);
                Files.write(uploadPath.resolve(safeFileName), encrypted);


                DemandePretDocument doc = new DemandePretDocument();
                doc.setNomFichier(safeFileName);
                doc.setDemandePret(demande);
                doc.setHash(hash);
                documentRepo.save(doc);
            }
            DemandePret scored = scorerDemande(id);
            System.out.println("🚨 [FRAUD] About to call analyzeAndUpdateFraudStatus");
            analyzeAndUpdateFraudStatus(scored);
            return scored;

        } catch (Exception e) {
            throw new RuntimeException("UPLOAD ERROR", e);
        }
    }


    @Override
    public byte[] getDocument(Long userId, long demandeId, String filename, String data, String sig) {

        try {
            if (!validate(userId, demandeId, filename, data, sig)) {
                throw new RuntimeException("Access denied");
            }

            Path filePath = Paths.get(UPLOAD_DIR, String.valueOf(demandeId), filename);

            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found");
            }

            DemandePret demande = demandePretRepo.findById(demandeId)
                    .orElseThrow(() -> new RuntimeException("Demande not found"));

            String dataKey = decryptDataKey(demande.getEncryptedDataKey());

            byte[] encrypted = Files.readAllBytes(filePath);
            byte[] decrypted = CryptoService.decrypt(encrypted, dataKey);

            DocumentAccessLog log = new DocumentAccessLog();
            log.setDemandeId(demandeId);
            log.setFileName(filename);
            log.setAction("READ");
            log.setTimestamp(LocalDateTime.now());

            logRepo.save(log);


            return decrypted;

        } catch (Exception e) {
            throw new RuntimeException("READ ERROR", e);
        }
    }



    public String generateSignedUrl(Long userId, long demandeId, String filename, long validityMillis) {

        long expiresAt = System.currentTimeMillis() + validityMillis;

        String data = userId + ":" + demandeId + ":" + filename + ":" + expiresAt;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(getHmacKey());

            String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(data.getBytes())
            );

            String encodedData = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(data.getBytes());

            return "/api/demandePret/" + demandeId + "/documents/" + filename
                    + "?uid=" + userId
                    + "&data=" + encodedData
                    + "&sig=" + sig;

        } catch (Exception e) {
            throw new RuntimeException("SIGNED URL ERROR", e);
        }
    }



    private boolean validate(Long userId, long demandeId, String filename, String dataEncoded, String sig) {

        try {
            String data = new String(Base64.getUrlDecoder().decode(dataEncoded));
            String[] parts = data.split(":");

            if (parts.length != 4) return false;

            long uid = Long.parseLong(parts[0]);
            long did = Long.parseLong(parts[1]);
            String file = parts[2];
            long expiresAt = Long.parseLong(parts[3]);

            if (System.currentTimeMillis() > expiresAt) return false;
            if (uid != userId) return false;
            if (did != demandeId) return false;
            if (!file.equals(filename)) return false;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(getHmacKey());

            String expectedSig = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(data.getBytes())
            );

            return expectedSig.equals(sig);

        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public long countDemandesByService(Long serviceId) {
        return demandePretRepo.countByService_Id(serviceId);
    }

    @Override
    public List<DemandePret> getByServiceId(long serviceId) {
        return demandePretRepo.findByService_Id(serviceId);
    }

    @Override
    public List<String> getDocumentNames(long id) {
        return documentRepo.findByDemandePret_Id(id)
                .stream()
                .map(DemandePretDocument::getNomFichier)
                .toList();
    }

    public DemandePret scorerDemande(Long id) {

        DemandePret demande = demandePretRepo.findById(id).orElseThrow();

        try {
            String dataKey = decryptDataKey(demande.getEncryptedDataKey());

            List<DemandePretDocument> docs = documentRepo.findByDemandePret_Id(id);

            List<byte[]> decryptedFiles = new ArrayList<>();
            List<String> fileNames      = new ArrayList<>();

            for (DemandePretDocument doc : docs) {
                Path path = Paths.get("uploads/demandes/" + id + "/" + doc.getNomFichier());
                byte[] encrypted = Files.readAllBytes(path);
                byte[] decrypted = CryptoService.decrypt(encrypted, dataKey);

                decryptedFiles.add(decrypted);
                fileNames.add(doc.getNomFichier()); // ex: "1732456789_cin.pdf"
            }


            Map<String, Object> result = scoringClientService.callScoringAPI(
                    decryptedFiles,
                    fileNames,
                    demande.getService().getId(),
                    demande.getMontantDemande(),
                    demande.getDureeMois()
            );

            System.out.println("SCORING RESPONSE = " + result);

            Number score    = (Number) result.get("score_solvabilite");
            String decision = (String) result.get("decision");

            if (score == null || decision == null) {
                System.out.println("⚠️ API ERROR: " + result);
                demande.setScoreSolvabilite(0);
                demande.setDecision("ERREUR API");
                return demandePretRepo.save(demande);
            }

            demande.setScoreSolvabilite(score.intValue());
            demande.setDecision(decision);

            return demandePretRepo.save(demande);

        } catch (Exception e) {
            e.printStackTrace();
            demande.setScoreSolvabilite(0);
            demande.setDecision("ERREUR SCORING");
            return demandePretRepo.save(demande);
        }
    }
    public DemandePret refuserDemande(Long id, String motif) {

        DemandePret demande = demandePretRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande not found"));

        demande.setStatut(StatutDemande.REJETEE);
        demande.setDecision("REFUSÉ");
        demande.setMotifRejet(motif);

        return demandePretRepo.save(demande);
    }
    public DemandePretDTO toDTO(DemandePret d) {

        DemandePretDTO dto = new DemandePretDTO();

        dto.setId(d.getId());
        dto.setMontantDemande(d.getMontantDemande());
        dto.setDureeMois(d.getDureeMois());
        dto.setDateDemande(d.getDateDemande());
        dto.setScoreSolvabilite(d.getScoreSolvabilite());
        dto.setDecision(d.getDecision());
        dto.setStatut(
                d.getStatut() != null ? d.getStatut().name() : "EN_ATTENTE"
        );

        dto.setServiceName(
                d.getService() != null ? d.getService().getNom() : "No service"
        );


        User user = null;
        try {
            user = userClient.getUser(d.getAgriculteurId());
        } catch (Exception e) {
            System.out.println("Feign error: " + e.getMessage());
        }

        dto.setFarmerName(user != null ? user.getNom() : "Unknown");
        dto.setFarmerLastName(user != null ? user.getPrenom() : "Unknown");

        dto.setFraudRiskLevel(d.getFraudRiskLevel());
        dto.setFraudScore(d.getFraudScore());
        dto.setFraudConfirmed(d.getFraudConfirmed());
        dto.setFraudAnalysisResult(d.getFraudAnalysisResult());

        return dto;
    }
    @Override
    public List<DemandePret> getDemandesByAgriculteur(Long agriculteurId) {
        return demandePretRepo.findByAgriculteurIdOrderByDateDemandeDesc(agriculteurId);
    }

    private void analyzeAndUpdateFraudStatus(DemandePret demande) {
        try {
            // Récupérer les documents déchiffrés
            String dataKey = decryptDataKey(demande.getEncryptedDataKey());
            List<DemandePretDocument> docs = documentRepo.findByDemandePret_Id(demande.getId());

            List<byte[]> decryptedFiles = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();

            for (DemandePretDocument doc : docs) {
                Path path = Paths.get("uploads/demandes/" + demande.getId() + "/" + doc.getNomFichier());
                byte[] encrypted = Files.readAllBytes(path);
                byte[] decrypted = CryptoService.decrypt(encrypted, dataKey);
                decryptedFiles.add(decrypted);
                fileNames.add(doc.getNomFichier());
            }

            // Analyse fraud
            FraudAnalysisResult fraudResult = fraudDetectionService.analyzeDocuments(
                    decryptedFiles,
                    fileNames,
                    demande.getAgriculteurId(),
                    demande.getId()
            );

            // Mettre à jour la demande avec les résultats fraud
            demande.setFraudRiskLevel(fraudResult.getGlobalRisk());
            demande.setFraudScore(fraudResult.getGlobalScore());
            demande.setFraudConfirmed(fraudResult.isFraudConfirmed());
            demande.setFraudAnalysisResult(convertToJson(fraudResult));

            // Ajuster la décision en fonction de la fraude
            if (fraudResult.isFraudConfirmed() || "HIGH".equals(fraudResult.getGlobalRisk())) {
                demande.setDecision("REFUSE_FRAUDE");
                demande.setStatut(StatutDemande.REJETEE);
                demande.setMotifRejet("Fraude documentaire détectée: " +
                        fraudResult.getRecommendationJustification());
            } else if ("MEDIUM".equals(fraudResult.getGlobalRisk())) {
                demande.setDecision("EXAMEN_MANUEL");
                demande.setStatut(StatutDemande.EN_ATTENTE);
            }

            demandePretRepo.save(demande);

        } catch (Exception e) {
            System.err.println("Fraud analysis error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String convertToJson(FraudAnalysisResult result) {
        try {
            return new ObjectMapper().writeValueAsString(result);
        } catch (Exception e) {
            return "{}";
        }
    }
}