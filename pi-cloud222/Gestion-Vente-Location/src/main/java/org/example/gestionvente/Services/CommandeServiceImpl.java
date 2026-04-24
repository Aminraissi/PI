package org.example.gestionvente.Services;

import jakarta.transaction.Transactional;
import org.example.gestionvente.Dtos.CartItemDto;
import org.example.gestionvente.Dtos.CommandeDetailDto;
import org.example.gestionvente.Dtos.CommandeHistoryDto;
import org.example.gestionvente.Entities.*;
import org.example.gestionvente.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommandeServiceImpl implements ICommandeService {

    @Autowired
    private CommandeRepo commandeRepo;

    @Autowired
    private PanierRepo panierRepo;

    @Autowired
    private LignePanierProduitRepo ligneRepo;

    @Autowired
    private IPanierService panierService;
    @Autowired
    private ProduitAgricoleRepo produitRepo;

    @Autowired
    private CommandeLigneRepo commandeLigneRepo;

    @Override
    @Transactional
    public Commande checkout(Long userId, Double tip) {

        Panier panier = panierService.getOrCreatePanier(userId);
        List<LignePanierProduit> lignes = ligneRepo.findByPanierId(panier.getId());

        if (lignes.isEmpty()) {
            throw new RuntimeException("Panier vide");
        }

        // 1) Vérifier stock
        for (LignePanierProduit ligne : lignes) {
            ProduitAgricole produit = produitRepo.findById(ligne.getProduitId())
                    .orElseThrow(() -> new RuntimeException("Produit introuvable: " + ligne.getProduitId()));

            if (ligne.getQuantite() > produit.getQuantiteDisponible()) {
                throw new RuntimeException(
                        "Stock insuffisant pour " + produit.getNom() +
                                ". Disponible: " + produit.getQuantiteDisponible() + " KG"
                );
            }
        }

        // 2) Décrémenter stock
        for (LignePanierProduit ligne : lignes) {
            ProduitAgricole produit = produitRepo.findById(ligne.getProduitId())
                    .orElseThrow(() -> new RuntimeException("Produit introuvable: " + ligne.getProduitId()));

            produit.setQuantiteDisponible(produit.getQuantiteDisponible() - ligne.getQuantite());
            produitRepo.save(produit);
        }

        // 3) Créer commande EN_COURS
        Commande commande = new Commande();
        commande.setUserId(userId);
        commande.setDateCommande(LocalDateTime.now());
        double sousTotal = panier.getMontantEstime() != null ? panier.getMontantEstime() : 0.0;
        double commission = sousTotal * 0.20;
        double safeTip = tip != null && tip > 0 ? tip : 0.0;
        double totalToPay = sousTotal + commission + safeTip;

        commande.setSousTotal(sousTotal);
        commande.setCommission(commission);
        commande.setTip(safeTip);
        commande.setMontantTotal(totalToPay);
        commande.setStatut("EN_COURS");
        commande.setPanierId(panier.getId());

        Commande savedCommande = commandeRepo.save(commande);

        // 3.1) Sauvegarder les lignes de commande
        for (LignePanierProduit ligne : lignes) {
            CommandeLigne ligneCommande = new CommandeLigne();
            ligneCommande.setCommandeId(savedCommande.getId());
            ligneCommande.setProduitId(ligne.getProduitId());
            ligneCommande.setQuantite(ligne.getQuantite());
            ligneCommande.setPrixUnitaire(ligne.getPrixUnitaire());
            ligneCommande.setSousTotal(ligne.getSousTotal());

            commandeLigneRepo.save(ligneCommande);
        }

        // 4) Geler le panier checkouté
        panier.setStatut("CHECKED_OUT");
        panierRepo.save(panier);

        return savedCommande;
    }

    @Override
    @Transactional
    public Commande validateCommande(Long commandeId) {
        Commande commande = commandeRepo.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        if (!"EN_COURS".equals(commande.getStatut())) {
            throw new RuntimeException("Seules les commandes EN_COURS peuvent être validées");
        }

        commande.setStatut("VALIDEE");
        Commande saved = commandeRepo.save(commande);

        if (commande.getPanierId() != null) {
            Panier panier = panierRepo.findById(commande.getPanierId())
                    .orElseThrow(() -> new RuntimeException("Panier introuvable"));
            panier.setStatut("VALIDEE");
            panierRepo.save(panier);
        }

        return saved;
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 30000) // toutes les 30 secondes
    public void cancelExpiredCommandes() {

        LocalDateTime limite = LocalDateTime.now().minusMinutes(2);

        List<Commande> expired = commandeRepo.findByStatutAndDateCommandeBefore("EN_COURS", limite);

        for (Commande commande : expired) {

            if (commande.getPanierId() == null) continue;

            Panier panier = panierRepo.findById(commande.getPanierId())
                    .orElse(null);

            if (panier == null) continue;

            List<LignePanierProduit> lignes = ligneRepo.findByPanierId(panier.getId());

            // remettre stock
            for (LignePanierProduit ligne : lignes) {
                ProduitAgricole produit = produitRepo.findById(ligne.getProduitId())
                        .orElse(null);

                if (produit != null) {
                    produit.setQuantiteDisponible(
                            produit.getQuantiteDisponible() + ligne.getQuantite()
                    );
                    produitRepo.save(produit);
                }
            }

            commande.setStatut("ANNULEE");
            commandeRepo.save(commande);

            panier.setStatut("ANNULE");
            panierRepo.save(panier);
        }
    }

    @Override
    @Transactional
    public List<CommandeHistoryDto> getPaidOrdersByUser(Long userId) {
        List<Commande> commandes = commandeRepo.findByUserIdAndStatutOrderByDateCommandeDesc(userId, "VALIDEE");

        List<CommandeHistoryDto> result = new ArrayList<>();

        for (Commande commande : commandes) {
            CommandeHistoryDto dto = new CommandeHistoryDto();
            dto.setCommandeId(commande.getId());
            dto.setPanierId(commande.getPanierId());
            dto.setDateCommande(commande.getDateCommande());
            dto.setStatut(commande.getStatut());
            dto.setMontantTotal(commande.getMontantTotal());
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public CommandeDetailDto getPaidOrderDetails(Long userId, Long commandeId) {
        Commande commande = commandeRepo.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        if (!commande.getUserId().equals(userId)) {
            throw new RuntimeException("Accès refusé à cette commande");
        }

        if (!"VALIDEE".equals(commande.getStatut())) {
            throw new RuntimeException("Seules les commandes payées peuvent être consultées");
        }

        if (commande.getPanierId() == null) {
            throw new RuntimeException("Aucun panier associé à cette commande");
        }

        List<LignePanierProduit> lignes = ligneRepo.findByPanierId(commande.getPanierId());
        List<CartItemDto> items = new ArrayList<>();

        for (LignePanierProduit ligne : lignes) {
            ProduitAgricole produit = produitRepo.findById(ligne.getProduitId()).orElse(null);

            CartItemDto item = new CartItemDto();
            item.setProduitId(ligne.getProduitId());
            item.setQuantite(ligne.getQuantite());
            item.setPrixUnitaire(ligne.getPrixUnitaire());
            item.setSousTotal(ligne.getSousTotal());

            if (produit != null) {
                item.setNom(produit.getNom());
                item.setImage(produit.getPhotoProduit());
                item.setStockDisponible(produit.getQuantiteDisponible());
            }

            items.add(item);
        }

        CommandeDetailDto detail = new CommandeDetailDto();
        detail.setCommandeId(commande.getId());
        detail.setPanierId(commande.getPanierId());
        detail.setDateCommande(commande.getDateCommande());

        detail.setSousTotal(commande.getSousTotal());
        detail.setCommission(commande.getCommission());
        detail.setTip(commande.getTip());
        detail.setMontantTotal(commande.getMontantTotal());

        detail.setStatut(commande.getStatut());
        detail.setItems(items);

        return detail;
    }


    @Override
    @Transactional
    public List<Commande> getAllOrdersForAdmin() {
        return commandeRepo.findAll();
    }

    @Override
    @Transactional
    public CommandeDetailDto getOrderDetailsForAdmin(Long commandeId) {
        Commande commande = commandeRepo.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        if (commande.getPanierId() == null) {
            throw new RuntimeException("Aucun panier associé à cette commande");
        }

        List<LignePanierProduit> lignes = ligneRepo.findByPanierId(commande.getPanierId());
        List<CartItemDto> items = new ArrayList<>();

        for (LignePanierProduit ligne : lignes) {
            ProduitAgricole produit = produitRepo.findById(ligne.getProduitId()).orElse(null);

            CartItemDto item = new CartItemDto();
            item.setProduitId(ligne.getProduitId());
            item.setQuantite(ligne.getQuantite());
            item.setPrixUnitaire(ligne.getPrixUnitaire());
            item.setSousTotal(ligne.getSousTotal());

            if (produit != null) {
                item.setNom(produit.getNom());
                item.setImage(produit.getPhotoProduit());
                item.setStockDisponible(produit.getQuantiteDisponible());
            }

            items.add(item);
        }

        CommandeDetailDto detail = new CommandeDetailDto();
        detail.setCommandeId(commande.getId());
        detail.setPanierId(commande.getPanierId());
        detail.setDateCommande(commande.getDateCommande());
        detail.setMontantTotal(commande.getMontantTotal());
        detail.setStatut(commande.getStatut());
        detail.setItems(items);

        return detail;
    }

}