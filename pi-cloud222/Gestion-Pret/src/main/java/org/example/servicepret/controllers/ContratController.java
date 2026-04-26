package org.example.servicepret.controllers;

import lombok.AllArgsConstructor;

import org.example.servicepret.DTO.ContratResponseDTO;
import org.example.servicepret.entities.Contrat;
import org.example.servicepret.entities.StatutContrat;
import org.example.servicepret.services.IContratService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/contrat")
public class ContratController {

    private IContratService contratService ;

    @GetMapping("/getAll")
    public List<Contrat> recupererTousLesContrats()
    {
        return contratService.retrieveContrats();
    }
    @PostMapping("/add")
    public Contrat ajouterContrat(@RequestBody Contrat c)
    {
        return contratService.addContrat(c);
    }

    @PutMapping("/update")
    public Contrat updateContrat(@RequestBody Contrat c)
    {
        return contratService.updateContrat(c);
    }
    @GetMapping("/get/{id}")
    public Contrat retrieveContrat(@PathVariable long id){
        return contratService.retrieveContrat(id);
    }

    @DeleteMapping("/delete/{id}")
    public void supprimerContrat(@PathVariable long id) {
        contratService.removeContrat(id);
    }

    @PostMapping("/generate/{demandeId}")
    public Contrat generate(@PathVariable Long demandeId) {
        return contratService.generateFromDemande(demandeId);
    }
    @PostMapping("/sign")
    public ResponseEntity<?> sign(@RequestBody Map<String, Object> body) {

        if (body == null || body.get("contratId") == null || body.get("signatureBase64") == null) {
            return ResponseEntity.badRequest().body("Missing data");
        }

        Long contratId = Long.valueOf(body.get("contratId").toString());
        String signatureBase64 = body.get("signatureBase64").toString();

        return ResponseEntity.ok(
                contratService.signContrat(contratId, signatureBase64)
        );
    }
    @GetMapping("/by-demande/{demandeId}")
    public Contrat getByDemande(@PathVariable Long demandeId) {
        return contratService.getByDemandeId(demandeId);
    }

    @GetMapping("/getContrat/{id}")
    public ResponseEntity<ContratResponseDTO> getContrat(@PathVariable Long id) {
        return ResponseEntity.ok(contratService.getContrat(id));
    }
    @PostMapping("/sign-with-pdf")
    public ResponseEntity<?> signWithPdf(
            @RequestParam("contratId") Long contratId,
            @RequestParam("signatureBase64") String signatureBase64,
            @RequestParam("pdfFile") MultipartFile pdfFile) {

        try {
            Contrat contrat = contratService.signContratWithPDF(contratId, signatureBase64, pdfFile);
            return ResponseEntity.ok(contrat);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/download-pdf/{contratId}")
    public ResponseEntity<byte[]> downloadPDF(@PathVariable Long contratId) {
        try {
            byte[] pdfBytes = contratService.getContratPDF(contratId);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "inline; filename=contrat_" + contratId + ".pdf")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/validated-contracts")
    public List<ContratResponseDTO> getValidatedContracts() {
        return contratService.getValidatedContracts();
    }

    @GetMapping("/rejected-contracts")
    public List<ContratResponseDTO> getRejectedContracts() {
        return contratService.getRejectedContracts();
    }

    @GetMapping("/all-with-details")
    public List<Contrat> getAllContractsWithDetails() {
        return contratService.getAllContractsWithDetails();
    }

    @GetMapping("/by-status/{statut}")
    public List<Contrat> getContractsByStatus(@PathVariable StatutContrat statut) {
        return contratService.getContractsByStatus(statut);
    }
    @GetMapping("/pending-validation")
    public List<ContratResponseDTO> getPendingContracts() {
        return contratService.getContratsEnAttenteValidation();
    }
    @PostMapping("/validate/{contratId}")
    public ResponseEntity<?> validateContract(
            @PathVariable Long contratId,
            @RequestBody Map<String, Object> body) {
        try {
            boolean valide = (boolean) body.getOrDefault("valide", false);
            Contrat contrat = contratService.validerContratParAdmin(contratId, valide);
            return ResponseEntity.ok(contrat);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }







}
