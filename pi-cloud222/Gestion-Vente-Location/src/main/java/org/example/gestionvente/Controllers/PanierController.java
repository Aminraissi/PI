package org.example.gestionvente.Controllers;

import org.example.gestionvente.Entities.Panier;
import org.example.gestionvente.Services.IPanierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.example.gestionvente.Dtos.PanierDetailDto;

@RestController
@RequestMapping("/api/panier")
public class PanierController {

    @Autowired
    private IPanierService service;

    @GetMapping("/{userId}")
    public Panier getPanier(@PathVariable("userId") Long userId) {
        return service.getOrCreatePanier(userId);
    }

    @GetMapping("/{userId}/details")
    public PanierDetailDto getPanierDetails(@PathVariable("userId") Long userId) {
        return service.getPanierDetails(userId);
    }
}