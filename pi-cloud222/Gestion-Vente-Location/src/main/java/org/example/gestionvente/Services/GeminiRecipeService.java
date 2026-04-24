package org.example.gestionvente.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.example.gestionvente.Dtos.RecipeCartResponse;
import org.example.gestionvente.Dtos.RecipeIngredientMatchDto;
import org.example.gestionvente.Entities.ProduitAgricole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class GeminiRecipeService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String cleanJsonResponse(String text) {
        if (text == null) {
            return "";
        }

        String cleaned = text.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        return cleaned;
    }
    public RecipeCartResponse generateRecipeFromProducts(String userPrompt, List<ProduitAgricole> produits) {
        try {
            if (geminiApiKey == null || geminiApiKey.isBlank()) {
                throw new IllegalStateException("gemini.api.key is not configured");
            }

            StringBuilder catalog = new StringBuilder();
            for (ProduitAgricole p : produits) {
                if (p.getQuantiteDisponible() != null && p.getQuantiteDisponible() > 0) {
                    catalog.append("- ID: ").append(p.getId())
                            .append(", name: ").append(p.getNom())
                            .append(", category: ").append(p.getCategory())
                            .append(", price_per_kg: ").append(p.getPrix())
                            .append(", stock_kg: ").append(p.getQuantiteDisponible())
                            .append("\n");
                }
            }

            String prompt = """
                You are a recipe-to-cart assistant for an agriculture marketplace.

                The user request is:
                %s

                Here is the list of available products on the site:
                %s

                Return ONLY valid JSON in this exact structure:
                {
                  "recipeTitle": "...",
                  "recipeDescription": "...",
                  "instructions": "...",
                  "ingredients": [
                    {
                      "produitId": 1,
                      "ingredientName": "tomato",
                      "requestedQuantityKg": 1.0
                    }
                  ],
                  "missingIngredients": ["salt"]
                }

                    Rules:
                    - You are ONLY a food recipe and food cart assistant.
                    - If the user asks for something unrelated to food, cooking, meals, ingredients, or edible products, return ONLY this JSON:
                    {
                      "recipeTitle": "Unsupported Request",
                      "recipeDescription": "This assistant only supports food and recipe related requests.",
                      "instructions": "Please ask for a recipe, meal idea, or food cart suggestion.",
                      "ingredients": [],
                      "missingIngredients": []
                    }
                    - Use only products from the provided catalog.
                    - produitId must be one from the catalog.
                    - requestedQuantityKg must be a whole integer number in KG only (1, 2, 3, ...).
                    - Do not return decimal quantities like 0.25, 0.5, 1.5.
                    - Minimum allowed quantity is 1 KG.
                    - Keep the recipe realistic.
                    - Prefer simple recipes using available products.
                    - Return ONLY raw JSON.
                    - Do not use markdown.
                    - Do not wrap the JSON in triple backticks.
                """.formatted(userPrompt, catalog);

            Client client = Client.builder().apiKey(geminiApiKey).build();
            GenerateContentResponse response =
                    client.models.generateContent("gemini-2.5-flash", prompt, null);

            String text = response.text();
            String cleanedText = cleanJsonResponse(text);

            System.out.println("RAW GEMINI RESPONSE: " + text);
            System.out.println("CLEANED GEMINI RESPONSE: " + cleanedText);

            JsonNode root = objectMapper.readTree(cleanedText);



            RecipeCartResponse result = new RecipeCartResponse();
            result.setRecipeTitle(root.path("recipeTitle").asText(""));
            result.setRecipeDescription(root.path("recipeDescription").asText(""));
            result.setInstructions(root.path("instructions").asText(""));
            result.setRawModelResponse(text);

            List<RecipeIngredientMatchDto> items = new ArrayList<>();
            JsonNode ingredients = root.path("ingredients");
            if (ingredients.isArray()) {
                for (JsonNode ing : ingredients) {
                    RecipeIngredientMatchDto dto = new RecipeIngredientMatchDto();
                    dto.setProduitId(ing.path("produitId").isMissingNode() ? null : ing.path("produitId").asLong());
                    dto.setIngredientName(ing.path("ingredientName").asText(""));
                    dto.setRequestedQuantityKg(ing.path("requestedQuantityKg").asDouble(0.0));
                    items.add(dto);
                }
            }

            List<String> missing = new ArrayList<>();
            JsonNode missingNode = root.path("missingIngredients");
            if (missingNode.isArray()) {
                for (JsonNode m : missingNode) {
                    missing.add(m.asText());
                }
            }

            result.setItems(items);
            result.setMissingIngredients(missing);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Erreur Gemini: " + e.getMessage(), e);
        }
    }
}
