package org.example.servicepret.controllers;

import lombok.AllArgsConstructor;
import org.example.servicepret.entities.Echeance;
import org.example.servicepret.services.IEcheanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/echeance")
public class EcheanceController {

    private IEcheanceService echeanceService;

    @GetMapping("/getAll")
    public List<Echeance> recupererTousLesEcheances()
    {
        return echeanceService.retrieveAllEcheances();
    }
    @PostMapping("/add")
    public Echeance ajouterEcheance(@RequestBody Echeance e)
    {
        return echeanceService.addEcheance(e);
    }

    @PutMapping("/update")
    public Echeance updateEcheance(@RequestBody Echeance e)
    {
        return echeanceService.updateEcheance(e);
    }
    @GetMapping("/get/{id}")
    public Echeance retrieveEcheance(@PathVariable long id)
    {
        return echeanceService.retrieveEcheance(id);
    }

    @DeleteMapping("/delete/{id}")
    public void supprimerEcheance(@PathVariable long id) {

       echeanceService.removeEcheance(id);
    }
    @GetMapping("/by-pret/{pretId}")
    public List<Echeance> getByPret(@PathVariable Long pretId) {
        return echeanceService.findByPretId(pretId);
    }

    @PostMapping("/payer/{pretId}")
    public void payer(@PathVariable Long pretId) {
        echeanceService.payerUneEcheance(pretId);
    }

    @GetMapping("/next/{pretId}")
    public Echeance getNext(@PathVariable Long pretId) {
        return echeanceService.getNextPayableEcheance(pretId);
    }



}
