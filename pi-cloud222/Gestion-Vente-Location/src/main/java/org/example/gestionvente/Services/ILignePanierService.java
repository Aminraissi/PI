package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.LignePanierProduit;

public interface ILignePanierService {
    LignePanierProduit addProduit(Long userId, Long produitId, Double quantite);
    LignePanierProduit updateQuantite(Long userId, Long produitId, Double quantite);
    void removeProduit(Long userId, Long produitId);
}