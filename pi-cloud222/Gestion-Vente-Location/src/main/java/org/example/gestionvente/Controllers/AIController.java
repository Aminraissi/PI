package org.example.gestionvente.Controllers;

import org.example.gestionvente.Entities.LignePanierProduit;
import org.example.gestionvente.Entities.ProduitAgricole;
import org.example.gestionvente.Repositories.LignePanierProduitRepo;
import org.example.gestionvente.Repositories.ProduitAgricoleRepo;
import org.example.gestionvente.Services.AIService;
import org.example.gestionvente.Services.IPanierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;
    @Autowired
    private IPanierService panierService;
    @Autowired
    private LignePanierProduitRepo ligneRepo;
    @Autowired
    private ProduitAgricoleRepo produitRepo;


    @GetMapping("/recommend/{userId}")
    public List<ProduitAgricole> recommend(@PathVariable("userId") Long userId) {

        // 🔥 1. Get cart
        List<LignePanierProduit> lignes = ligneRepo.findByPanierId(
                panierService.getOrCreatePanier(userId).getId()
        );

        // 🔥 2. Get product IDs already in cart
        List<Long> cartProductIds = lignes.stream()
                .map(LignePanierProduit::getProduitId)
                .toList();

        // 🔥 3. Get all products NOT in cart
        List<ProduitAgricole> allProducts = produitRepo.findAll();

        List<ProduitAgricole> filtered = allProducts.stream()
                .filter(p -> !cartProductIds.contains(p.getId()))
                .collect(Collectors.toList());

        // 🔥 4. Shuffle (random)
        Collections.shuffle(filtered);

        // 🔥 5. Limit results
        return filtered.stream()
                .limit(6)
                .toList();
    }


    @GetMapping("/product-recommend/{productId}")
    public List<ProduitAgricole> recommendByProduct(
            @PathVariable("productId") Long productId) {

        ProduitAgricole product = produitRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));


        System.out.println("🚨 ENTERED AI CONTROLLER");
        // 🔥 1. call AI
        List<String> names = aiService.getProductRecommendations(
                product.getNom(),
                product.getCategory()
        );
        System.out.println("🧠 AI KEYWORDS: " + names);

        // 🔥 2. match DB
        List<ProduitAgricole> all = produitRepo.findAll();

        List<ProduitAgricole> result = all.stream()
                .filter(p -> !p.getId().equals(productId))
                .filter(p -> names.stream().anyMatch(n ->
                        p.getNom().toLowerCase().contains(n.toLowerCase()) ||
                                n.toLowerCase().contains(p.getNom().toLowerCase())
                ))
                .limit(5)
                .toList();

        System.out.println("📦 MATCHED PRODUCTS: " +
                result.stream().map(ProduitAgricole::getNom).toList());
        // 🔥 fallback if AI fails
        if (result.isEmpty()) {
            System.out.println("⚠️ AI FAILED → USING RANDOM PRODUCTS");
            Collections.shuffle(all);
            return all.stream()
                    .filter(p -> !p.getId().equals(productId))
                    .limit(5)
                    .toList();
        }

        return result;
    }
}