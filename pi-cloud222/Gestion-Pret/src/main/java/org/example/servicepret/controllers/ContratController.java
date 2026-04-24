package org.example.servicepret.controllers;

import lombok.AllArgsConstructor;

import org.example.servicepret.DTO.ContratResponseDTO;
import org.example.servicepret.entities.Contrat;
import org.example.servicepret.services.IContratService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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




}
