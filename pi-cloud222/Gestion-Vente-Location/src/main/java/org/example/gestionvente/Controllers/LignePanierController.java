package org.example.gestionvente.Controllers;

import org.example.gestionvente.Entities.LignePanierProduit;
import org.example.gestionvente.Services.ILignePanierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/panier")
public class LignePanierController {

    @Autowired
    private ILignePanierService service;

    @PostMapping("/add")
    public LignePanierProduit addProduit(
            @RequestParam("userId") Long userId,
            @RequestParam("produitId") Long produitId,
            @RequestParam("quantite") Double quantite) {

        return service.addProduit(userId, produitId, quantite);

    }

    @PutMapping("/update")
    public LignePanierProduit updateQuantite(
            @RequestParam("userId") Long userId,
            @RequestParam("produitId") Long produitId,
            @RequestParam("quantite") Double quantite) {

        return service.updateQuantite(userId, produitId, quantite);
    }
    @DeleteMapping("/remove")
    public void removeProduit(
            @RequestParam("userId") Long userId,
            @RequestParam("produitId") Long produitId) {

        service.removeProduit(userId, produitId);
    }
}