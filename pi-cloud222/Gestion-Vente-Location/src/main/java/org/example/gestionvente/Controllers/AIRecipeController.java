package org.example.gestionvente.Controllers;

import org.example.gestionvente.Dtos.RecipeCartRequest;
import org.example.gestionvente.Dtos.RecipeCartResponse;
import org.example.gestionvente.Services.AIRecipeCartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIRecipeController {

    private final AIRecipeCartService aiRecipeCartService;

    public AIRecipeController(AIRecipeCartService aiRecipeCartService) {
        this.aiRecipeCartService = aiRecipeCartService;
    }

    @PostMapping("/recipe-cart")
    public RecipeCartResponse recipeCart(@RequestBody RecipeCartRequest request) {
        return aiRecipeCartService.generateRecipeAndMaybeFillCart(request);
    }
}