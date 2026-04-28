package org.example.gestioninventaire.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqModerationService {

    private static final String GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final ObjectMapper objectMapper;

    @Value("${moderation.groq.enabled:${groq.enabled:true}}")
    private boolean moderationEnabled;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${moderation.groq.model:${groq.model:llama-3.3-70b-versatile}}")
    private String groqModel;

    @Value("${moderation.groq.fail-closed:false}")
    private boolean failClosed;

    public TextModerationResult moderate(String text) {
        if (!isConfigured() || text == null || text.isBlank()) {
            return TextModerationResult.allowed("groq-skipped");
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            Map<String, Object> body = Map.of(
                    "model", groqModel,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", """
                                            You are a strict content moderation classifier for user reviews.
                                            Return only valid JSON with this shape:
                                            {"allowed": true|false, "reason": "short reason in French"}
                                            Block insults, hate, threats, harassment, sexual abuse, and toxic language.
                                            Allow normal negative reviews that are polite and factual.
                                            """
                            ),
                            Map.of("role", "user", "content", text)
                    ),
                    "temperature", 0,
                    "max_tokens", 120
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    GROQ_CHAT_URL,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode moderation = objectMapper.readTree(extractJson(content));

            boolean allowed = moderation.path("allowed").asBoolean(true);
            String reason = moderation.path("reason").asText("Le contenu ne respecte pas les regles de la communaute.");

            return allowed
                    ? TextModerationResult.allowed("groq")
                    : TextModerationResult.blocked(reason, "groq");
        } catch (Exception e) {
            log.warn("Groq moderation unavailable: {}", e.getMessage());
            return failClosed
                    ? TextModerationResult.blocked("La moderation est temporairement indisponible.", "groq-error")
                    : TextModerationResult.allowed("groq-error-soft-pass");
        }
    }

    private boolean isConfigured() {
        return moderationEnabled && groqApiKey != null && !groqApiKey.isBlank();
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }
}
