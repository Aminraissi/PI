package org.example.servicepret.controllers;

import lombok.AllArgsConstructor;
import org.example.servicepret.DTO.PretRequestDTO;
import org.example.servicepret.entities.*;
import org.example.servicepret.services.IContratService;
import org.example.servicepret.services.IDemandePretService;
import org.example.servicepret.services.IEcheanceService;
import org.example.servicepret.services.IPretService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@AllArgsConstructor
@RequestMapping("/api/pret")
public class PretController {

    private IPretService pretService;
    private IContratService contratService;
    private IDemandePretService demandePretService;
    private IEcheanceService echeanceService;
    @GetMapping("/getAll")
    public List<Pret> recupererTousLesPrets()
    {
        return pretService.retrieveAllPrets();
    }
    @PostMapping("/create-after-payment")
    public ResponseEntity<Pret> createAfterPayment(@RequestBody PretRequestDTO dto) {


        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }


        DemandePret demande = demandePretService.retrieveDemande(dto.getDemandeId());

        if (demande == null) {
            throw new RuntimeException("Demande not found");
        }


        Contrat contrat = contratService.RecupererParDemande(demande.getId());
        if (contrat == null) {
            throw new RuntimeException("Contrat non trouvé pour cette demande");
        }

        Pret pret = new Pret();
        pret.setTauxInteret(dto.getTauxInteret());
        pret.setDateDebut(dto.getDateDebut());
        pret.setDateFin(dto.getDateFin());
        pret.setMontantTotal(dto.getMontantTotal());
        pret.setNbEcheances(dto.getNbEcheances());
        pret.setAgentId(dto.getAgentId());

        pret.setStatutPret(StatutPret.ACTIF);

        pret.setDemande(demande);
        pret.setContrat(contrat);

        demande.setStatut(StatutDemande.APPROUVEE);

        demandePretService.updateDemande(demande);

        Pret saved = pretService.addPret(pret);

        return ResponseEntity.ok(saved);
    }
    @PutMapping("/update")
    public Pret updatePret(@RequestBody Pret p)
    {
        return pretService.updatePret(p);
    }
    @GetMapping("/get/{id}")
    public Pret retrievePret(@PathVariable long id){return pretService.retrievePret(id);}

    @DeleteMapping("/delete/{id}")
    public void supprimerPret(@PathVariable long id) {pretService.removePret(id);}

    @GetMapping("/by-demande/{id}")
    public ResponseEntity<Pret> getByDemande(@PathVariable Long id) {

        System.out.println("Searching pret for demandeId = " + id);

        Pret pret = pretService.findByDemandeId(id);

        System.out.println("RESULT = " + pret);

        if (pret == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(pret);
    }
    @GetMapping("/agriculteur/{id}")
    public List<Pret> getPretsByAgriculteur(@PathVariable Long id) {
        return pretService.getPretByAgriculteur(id);
    }
}
