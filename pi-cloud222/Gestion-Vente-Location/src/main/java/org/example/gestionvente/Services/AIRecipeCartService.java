package org.example.gestionvente.Services;

import org.example.gestionvente.Dtos.RecipeCartRequest;
import org.example.gestionvente.Dtos.RecipeCartResponse;
import org.example.gestionvente.Dtos.RecipeIngredientMatchDto;
import org.example.gestionvente.Entities.ProduitAgricole;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AIRecipeCartService {

    private final IProduitAgricoleService produitService;
    private final ILignePanierService lignePanierService;
    private final GeminiRecipeService geminiRecipeService;

    public AIRecipeCartService(
            IProduitAgricoleService produitService,
            ILignePanierService lignePanierService,
            GeminiRecipeService geminiRecipeService
    ) {
        this.produitService = produitService;
        this.lignePanierService = lignePanierService;
        this.geminiRecipeService = geminiRecipeService;
    }

    public RecipeCartResponse generateRecipeAndMaybeFillCart(RecipeCartRequest request) {
        String prompt = request.getPrompt() == null ? "" : request.getPrompt().trim();

        if (!isFoodRelatedPrompt(prompt)) {
            RecipeCartResponse response = new RecipeCartResponse();
            response.setRecipeTitle("Unsupported Request");
            response.setRecipeDescription("This assistant is only for recipes and food cart suggestions.");
            response.setInstructions("Ask for a recipe, meal idea, ingredients, or food-based cart suggestion using marketplace products.");
            response.setEstimatedTotal(0.0);
            response.getMissingIngredients().add("Please enter a food or recipe related request.");
            return response;
        }
        List<ProduitAgricole> produits = produitService.findAll();

        RecipeCartResponse response =
                geminiRecipeService.generateRecipeFromProducts(request.getPrompt(), produits);

        double total = 0.0;

        for (RecipeIngredientMatchDto item : response.getItems()) {
            if (item.getProduitId() == null) {
                item.setMatched(false);
                continue;
            }

            Double normalizedQty = normalizeQuantity(item.getRequestedQuantityKg());
            item.setRequestedQuantityKg(normalizedQty);

            ProduitAgricole produit = produitService.findById(item.getProduitId());

            item.setProductName(produit.getNom());
            item.setAvailableQuantityKg(produit.getQuantiteDisponible());
            item.setUnitPrice(produit.getPrix());

            boolean enoughStock = item.getRequestedQuantityKg() != null
                    && item.getRequestedQuantityKg() > 0
                    && produit.getQuantiteDisponible() != null
                    && produit.getQuantiteDisponible() >= item.getRequestedQuantityKg();

            item.setMatched(enoughStock);

            if (enoughStock) {
                double cost = item.getRequestedQuantityKg() * produit.getPrix();
                item.setEstimatedCost(cost);
                total += cost;

                if (request.isAddToCart()) {
                    lignePanierService.addProduit(
                            request.getUserId(),
                            produit.getId(),
                            item.getRequestedQuantityKg()
                    );
                }
            } else {
                response.getMissingIngredients().add(item.getIngredientName());
            }
        }

        response.setEstimatedTotal(total);
        return response;
    }

    private boolean isFoodRelatedPrompt(String prompt) {
        String text = prompt.toLowerCase();

        String[] foodKeywords = {
                "recipe", "cook", "meal", "dish", "salad", "soup", "juice", "smoothie",
                "breakfast", "lunch", "dinner", "dessert", "ingredient", "food",
                "vegetable", "fruit", "legume", "oil", "dairy", "cheese", "milk",
                "tomato", "potato", "onion", "carrot", "pepper", "apple", "banana",
                "orange", "watermelon", "tunisian", "couscous", "ojja", "chakchouka"
        };

        for (String keyword : foodKeywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private Double normalizeQuantity(Double qty) {
        if (qty == null || qty <= 0) {
            return 1.0;
        }

        return (double) Math.max(1, (int) Math.ceil(qty));
    }
}