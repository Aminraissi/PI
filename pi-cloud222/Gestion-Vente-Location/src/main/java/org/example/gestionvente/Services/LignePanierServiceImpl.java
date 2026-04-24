package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.LignePanierProduit;
import org.example.gestionvente.Entities.Panier;
import org.example.gestionvente.Entities.ProduitAgricole;
import org.example.gestionvente.Repositories.LignePanierProduitRepo;
import org.example.gestionvente.Repositories.ProduitAgricoleRepo;
import org.example.gestionvente.Repositories.PanierRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LignePanierServiceImpl implements ILignePanierService {

    @Autowired
    private LignePanierProduitRepo ligneRepo;

    @Autowired
    private ProduitAgricoleRepo produitRepo;

    @Autowired
    private IPanierService panierService;

    @Autowired
    private PanierRepo panierRepo;

    @Autowired
    public LignePanierServiceImpl(LignePanierProduitRepo ligneRepo, ProduitAgricoleRepo produitRepo) {
        this.ligneRepo = ligneRepo;
        this.produitRepo = produitRepo;
    }
    @Override
    public LignePanierProduit addProduit(Long userId, Long produitId, Double quantite) {

        if (quantite == null || quantite <= 0) {
            throw new RuntimeException("La quantité doit être supérieure à 0");
        }

        // récupérer panier actif
        Panier panier = panierService.getOrCreatePanier(userId);

        // récupérer produit
        ProduitAgricole produit = produitRepo.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        // vérifier ligne existante
        LignePanierProduit ligne = ligneRepo
                .findByPanierIdAndProduitId(panier.getId(), produitId)
                .orElse(new LignePanierProduit());

        double ancienneQuantite = ligne.getQuantite() == null ? 0.0 : ligne.getQuantite();
        double nouvelleQuantite = ancienneQuantite + quantite;

        if (nouvelleQuantite > produit.getQuantiteDisponible()) {
            throw new RuntimeException(
                    "Quantité demandée indisponible. Stock disponible: " + produit.getQuantiteDisponible() + " KG"
            );
        }

        ligne.setPanierId(panier.getId());
        ligne.setProduitId(produitId);
        ligne.setQuantite(nouvelleQuantite);
        ligne.setPrixUnitaire(produit.getPrix());
        ligne.setSousTotal(nouvelleQuantite * produit.getPrix());

        LignePanierProduit saved = ligneRepo.save(ligne);

        updatePanierTotal(panier.getId());
        return saved;
    }


    @Override
    public LignePanierProduit updateQuantite(Long userId, Long produitId, Double quantite) {

        if (quantite == null || quantite <= 0) {
            throw new RuntimeException("La quantité doit être supérieure à 0");
        }

        Panier panier = panierService.getOrCreatePanier(userId);

        LignePanierProduit ligne = ligneRepo
                .findByPanierIdAndProduitId(panier.getId(), produitId)
                .orElseThrow(() -> new RuntimeException("Ligne introuvable"));

        ProduitAgricole produit = produitRepo.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        if (quantite > produit.getQuantiteDisponible()) {
            throw new RuntimeException(
                    "Quantité demandée indisponible. Stock disponible: " + produit.getQuantiteDisponible() + " KG"
            );
        }

        ligne.setQuantite(quantite);
        ligne.setPrixUnitaire(produit.getPrix());
        ligne.setSousTotal(quantite * produit.getPrix());

        LignePanierProduit updated = ligneRepo.save(ligne);

        updatePanierTotal(panier.getId());

        return updated;
    }

    private void updatePanierTotal(Long panierId) {

        Panier panier = panierRepo.findById(panierId)
                .orElseThrow(() -> new RuntimeException("Panier not found"));

        List<LignePanierProduit> lignes = ligneRepo.findByPanierId(panierId);

        double total = 0;

        for (LignePanierProduit l : lignes) {
            total += l.getSousTotal();
        }

        panier.setMontantEstime(total);
        panierRepo.save(panier);
    }


    public void removeProduit(Long userId, Long produitId) {

        Panier panier = panierService.getOrCreatePanier(userId);

        LignePanierProduit ligne = ligneRepo
                .findByPanierIdAndProduitId(panier.getId(), produitId)
                .orElseThrow(() -> new RuntimeException("Ligne not found"));

        ligneRepo.delete(ligne);

        updatePanierTotal(panier.getId());
    }
}