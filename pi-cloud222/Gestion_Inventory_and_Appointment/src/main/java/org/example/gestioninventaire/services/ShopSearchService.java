package org.example.gestioninventaire.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gestioninventaire.dtos.response.InventoryProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopSearchService {

    private final ProductCrudService productCrudService;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.model}")
    private String groqModel;

    /**
     * Uses Groq AI to interpret a natural language query and filter products.
     * Example: "vaccin pour bovins dans la région de Tunis"
     */
    public List<InventoryProductResponse> searchWithAI(String query) {
        List<InventoryProductResponse> allProducts = productCrudService.getAllPublicShop();

        // Build product catalog summary for Groq
        String catalog = allProducts.stream()
                .map(p -> String.format("ID:%d | %s | %s | %s | Région:%s | Prix:%.2f TND | Stock:%.0f %s",
                        p.getId(), p.getNom(),
                        p.getCategorie() != null ? p.getCategorie().name() : "",
                        p.getOwner() != null ? (p.getOwner().getPrenom() + " " + p.getOwner().getNom()) : "Vétérinaire",
                        p.getOwner() != null && p.getOwner().getRegion() != null ? p.getOwner().getRegion() : "Non précisée",
                        p.getPrixVente() != null ? p.getPrixVente() : 0.0,
                        p.getCurrentQuantity() != null ? p.getCurrentQuantity() : 0.0,
                        p.getUnit() != null ? p.getUnit() : ""))
                .collect(Collectors.joining("\n"));

        String prompt = """
                Tu es un assistant pour une plateforme vétérinaire agricole.
                Voici la liste des produits disponibles dans les boutiques des vétérinaires:
                
                %s
                
                Question de l'agriculteur: "%s"
                
                Analyse la question et retourne UNIQUEMENT une liste JSON des IDs des produits correspondants.
                Format: {"ids": [1, 2, 3]}
                Si aucun produit ne correspond, retourne: {"ids": []}
                Ne retourne que le JSON, rien d'autre.
                """.formatted(catalog, query);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);

            Map<String, Object> body = Map.of(
                    "model", groqModel,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", 200,
                    "temperature", 0.1
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions", entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // Extract JSON from response
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}') + 1;
            if (start >= 0 && end > start) {
                content = content.substring(start, end);
            }

            JsonNode result = objectMapper.readTree(content);
            JsonNode idsNode = result.path("ids");

            if (idsNode.isArray()) {
                List<Long> ids = objectMapper.readerForListOf(Long.class).readValue(idsNode);
                return allProducts.stream()
                        .filter(p -> ids.contains(p.getId()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Groq AI search error: {}", e.getMessage());
        }

        return List.of();
    }
}
