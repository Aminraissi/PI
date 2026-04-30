package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.Panier;
import org.example.gestionvente.Repositories.PanierRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import org.example.gestionvente.Entities.LignePanierProduit;
import org.example.gestionvente.Entities.ProduitAgricole;
import org.example.gestionvente.Repositories.LignePanierProduitRepo;
import org.example.gestionvente.Repositories.ProduitAgricoleRepo;
import org.example.gestionvente.Dtos.CartItemDto;
import org.example.gestionvente.Dtos.PanierDetailDto;

import java.util.ArrayList;
import java.util.List;

@Service
public class PanierServiceImpl implements IPanierService {

    private PanierRepo repository;
    private LignePanierProduitRepo ligneRepo;
    private ProduitAgricoleRepo produitRepo;
    public PanierServiceImpl(PanierRepo repository,LignePanierProduitRepo ligneRepo,
                             ProduitAgricoleRepo produitRepo) {
        this.repository = repository;
        this.ligneRepo = ligneRepo;
        this.produitRepo = produitRepo;
    }

    @Override
    public Panier getOrCreatePanier(Long idUser) {

        return repository.findByIdUserAndStatut(idUser, "ACTIF")
                .orElseGet(() -> {
                    Panier p = new Panier();
                    p.setIdUser(idUser);
                    p.setDateCreation(LocalDateTime.now());
                    p.setStatut("ACTIF");
                    p.setMontantEstime(0.0);
                    return repository.save(p);
                });
    }

    @Override
    public PanierDetailDto getPanierDetails(Long idUser) {

        Panier panier = getOrCreatePanier(idUser);

        List<LignePanierProduit> lignes = ligneRepo.findByPanierId(panier.getId());
        List<CartItemDto> items = new ArrayList<>();

        for (LignePanierProduit ligne : lignes) {
            ProduitAgricole produit = produitRepo.findById(ligne.getProduitId())
                    .orElse(null);

            CartItemDto dto = new CartItemDto();
            dto.setProduitId(ligne.getProduitId());
            dto.setPrixUnitaire(ligne.getPrixUnitaire());
            dto.setQuantite(ligne.getQuantite());
            dto.setSousTotal(ligne.getSousTotal());

            if (produit != null) {
                dto.setNom(produit.getNom());
                dto.setImage(produit.getPhotoProduit());
                dto.setStockDisponible(produit.getQuantiteDisponible());
            }

            items.add(dto);
        }

        PanierDetailDto detail = new PanierDetailDto();
        detail.setPanierId(panier.getId());
        detail.setUserId(panier.getIdUser());
        detail.setMontantEstime(panier.getMontantEstime());
        detail.setStatut(panier.getStatut());
        detail.setItems(items);

        return detail;
    }
}