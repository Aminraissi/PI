package org.exemple.assistance_service.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemple.assistance_service.dto.LLMResponseDTO;
import org.exemple.assistance_service.entity.DemandeAssistance;
import org.exemple.assistance_service.enums.TypeProbleme;
import org.exemple.assistance_service.exception.LLMException;
import org.exemple.assistance_service.service.LLMService;
import org.exemple.assistance_service.service.PromptBuilderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiLLMService implements LLMService {

    private static final String FALLBACK_WARNING = "AI suggestion only - contact an agricultural engineer or veterinarian for urgent cases.";

    private final PromptBuilderService promptBuilderService;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Override
    public LLMResponseDTO generateAssistanceResponse(DemandeAssistance demande) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LLMException("Gemini API key is not configured");
        }

        String prompt = promptBuilderService.buildAssistancePrompt(demande);
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json",
                        "responseSchema", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "diagnostic", Map.of("type", "STRING"),
                                        "probabilite", Map.of("type", "NUMBER"),
                                        "recommandations", Map.of("type", "STRING"),
                                        "besoinExpert", Map.of("type", "BOOLEAN")
                                ),
                                "required", List.of("diagnostic", "probabilite", "recommandations", "besoinExpert")
                        )
                )
        );

        try {
            String responseBody = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("x-goog-api-key", apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build()
                    .post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode response = objectMapper.readTree(responseBody);
            String content = extractContent(response);
            LLMResponseDTO parsed = objectMapper.readValue(content, LLMResponseDTO.class);
            return applySafetyRules(demande, parsed);
        } catch (LLMException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("Gemini API failure for demande id={} model={}", demande.getIdDemande(), model, ex);
            throw new LLMException("AI provider call failed", ex);
        } catch (Exception ex) {
            log.error("Invalid Gemini response for demande id={} model={}", demande.getIdDemande(), model, ex);
            throw new LLMException("AI provider returned an invalid JSON response", ex);
        }
    }

    @Override
    public String getModelName() {
        return model;
    }

    private String extractContent(JsonNode response) {
        JsonNode content = response == null ? null : response.at("/candidates/0/content/parts/0/text");
        if (content == null || content.isMissingNode() || content.asText().isBlank()) {
            JsonNode blockReason = response == null ? null : response.at("/promptFeedback/blockReason");
            if (blockReason != null && !blockReason.isMissingNode()) {
                throw new LLMException("Gemini blocked the prompt: " + blockReason.asText());
            }
            throw new LLMException("AI provider returned an empty response");
        }
        return content.asText();
    }

    private LLMResponseDTO applySafetyRules(DemandeAssistance demande, LLMResponseDTO response) {
        double confidence = Math.max(0.0, Math.min(1.0, response.getProbabilite()));
        boolean expertNeeded = response.isBesoinExpert()
                || confidence < 0.5
                || demande.getTypeProbleme() == TypeProbleme.MALADIE_ANIMALE;

        String recommandations = response.getRecommandations();
        if (expertNeeded && (recommandations == null || !recommandations.contains(FALLBACK_WARNING))) {
            recommandations = (recommandations == null || recommandations.isBlank())
                    ? FALLBACK_WARNING
                    : recommandations + "\n\n" + FALLBACK_WARNING;
        }

        return LLMResponseDTO.builder()
                .diagnostic(response.getDiagnostic())
                .probabilite(confidence)
                .recommandations(recommandations)
                .besoinExpert(expertNeeded)
                .build();
    }
}
