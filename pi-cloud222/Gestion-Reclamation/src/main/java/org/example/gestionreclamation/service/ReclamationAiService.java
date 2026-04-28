package org.example.gestionreclamation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gestionreclamation.dto.CorrectDescriptionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReclamationAiService {

    private static final String GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final ObjectMapper objectMapper;

    @Value("${groq.enabled:true}")
    private boolean groqEnabled;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${moderation.groq.fail-closed:true}")
    private boolean failClosed;

    public TextModerationResult moderate(String text) {
        if (text == null || text.isBlank()) {
            return TextModerationResult.allowed("groq-empty");
        }

        if (!isConfigured()) {
            return TextModerationResult.blocked(
                    "La moderation IA est indisponible. Configurez la cle Groq avant de soumettre une reclamation.",
                    "groq-not-configured"
            );
        }

        try {
            String content = chat(List.of(
                    Map.of(
                            "role", "system",
                            "content", """
                                    You are a strict content moderation classifier for customer claims.
                                    Return only valid JSON with this exact shape:
                                    {"allowed": true|false, "reason": "short reason in French"}
                                    Block harassment, threats, hate speech, disguised insults, direct personal attacks,
                                    humiliation, toxic language, and abusive content even when no exact banned word is present.
                                    Always block second-person abusive or humiliating statements aimed at a person.
                                    Block phrases like "you are a loser", "your opinion is worthless",
                                    "everyone should avoid you", and similar personal attacks.
                                    The sentence "Your opinion doesn't mean squat, you're a loser, and everyone should steer clear of you."
                                    must be classified as {"allowed": false}.
                                    Allow normal negative complaints when they are factual and polite.
                                    """
                    ),
                    Map.of("role", "user", "content", text)
            ), 0, 120);

            JsonNode moderation = objectMapper.readTree(extractJson(content));
            JsonNode allowedNode = moderation.get("allowed");
            if (allowedNode == null || !allowedNode.isBoolean()) {
                return TextModerationResult.blocked(
                        "La moderation IA a retourne une reponse invalide. Merci de reformuler.",
                        "groq-invalid-response"
                );
            }

            boolean allowed = allowedNode.asBoolean(false);
            String reason = moderation.path("reason").asText(
                    "Le contenu ne respecte pas les regles de la communaute."
            );

            log.info("Groq moderation decision: allowed={}, source=groq", allowed);

            return allowed
                    ? TextModerationResult.allowed("groq")
                    : TextModerationResult.blocked(reason, "groq");
        } catch (Exception e) {
            log.warn("Groq moderation unavailable: {}", e.getMessage());
            return failClosed
                    ? TextModerationResult.blocked("La moderation IA est temporairement indisponible.", "groq-error")
                    : TextModerationResult.allowed("groq-error-soft-pass");
        }
    }

    public String correctDescription(CorrectDescriptionRequest request) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IA indisponible: cle Groq manquante.");
        }

        String description = request.description() == null ? "" : request.description().trim();
        if (description.length() < 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La description doit contenir au moins 20 caracteres.");
        }

        TextModerationResult moderation = moderate(description);
        if (!moderation.allowed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, moderation.reason());
        }

        try {
            String content = chat(List.of(
                    Map.of(
                            "role", "system",
                            "content", """
                                    You correct customer claim descriptions.
                                    Return only valid JSON with this exact shape:
                                    {"description": "corrected description"}
                                    Rules:
                                    - Keep the same meaning and facts.
                                    - Do not invent order numbers, dates, names, proof, or new events.
                                    - Make the tone professional, clear, and polite.
                                    - Preserve the user's language when possible.
                                    - Remove insults or toxic phrasing by replacing it with neutral wording.
                                    """
                    ),
                    Map.of(
                            "role", "user",
                            "content", "Subject: " + nullToBlank(request.subject())
                                    + "\nCategory: " + nullToBlank(request.category())

                                    + "\nDescription:\n" + description
                    )
            ), 0.2, 300);

            JsonNode corrected = objectMapper.readTree(extractJson(content));
            String correctedDescription = corrected.path("description").asText("").trim();
            if (correctedDescription.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correction IA invalide.");
            }
            return correctedDescription;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Groq correction unavailable: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correction IA indisponible pour le moment.");
        }
    }

    private String chat(List<Map<String, String>> messages, double temperature, int maxTokens) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
                "model", groqModel,
                "messages", messages,
                "response_format", Map.of("type", "json_object"),
                "temperature", temperature,
                "max_tokens", maxTokens
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                GROQ_CHAT_URL,
                new HttpEntity<>(body, headers),
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("choices").path(0).path("message").path("content").asText();
    }

    private boolean isConfigured() {
        return groqEnabled && groqApiKey != null && !groqApiKey.isBlank();
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
