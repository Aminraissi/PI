package org.example.gestionvente.Dtos;

import java.util.ArrayList;
import java.util.List;

public class RecipeCartResponse {
    private String recipeTitle;
    private String recipeDescription;
    private String instructions;
    private Double estimatedTotal;
    private List<RecipeIngredientMatchDto> items = new ArrayList<>();
    private List<String> missingIngredients = new ArrayList<>();
    private String rawModelResponse;

    public String getRecipeTitle() { return recipeTitle; }
    public void setRecipeTitle(String recipeTitle) { this.recipeTitle = recipeTitle; }

    public String getRecipeDescription() { return recipeDescription; }
    public void setRecipeDescription(String recipeDescription) { this.recipeDescription = recipeDescription; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public Double getEstimatedTotal() { return estimatedTotal; }
    public void setEstimatedTotal(Double estimatedTotal) { this.estimatedTotal = estimatedTotal; }

    public List<RecipeIngredientMatchDto> getItems() { return items; }
    public void setItems(List<RecipeIngredientMatchDto> items) { this.items = items; }

    public List<String> getMissingIngredients() { return missingIngredients; }
    public void setMissingIngredients(List<String> missingIngredients) { this.missingIngredients = missingIngredients; }

    public String getRawModelResponse() { return rawModelResponse; }
    public void setRawModelResponse(String rawModelResponse) { this.rawModelResponse = rawModelResponse; }
}