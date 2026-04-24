package tn.esprit.livraison.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.livraison.controller.dto.FarmerChatbotResponse;

import tn.esprit.livraison.controller.dto.BestDayChatbotRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GroqChatbotProxyService {

    @Value("${app.groq.api-key:}")
    private String groqApiKey;

    @Value("${app.groq.model:llama-3.1-8b-instant}")
    private String groqModel;

    @Value("${app.groq.api-url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${app.groq.timeout-ms:12000}")
    private long groqTimeoutMs;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final WeatherPricingService weatherPricingService;

    public GroqChatbotProxyService(ObjectMapper objectMapper, WeatherPricingService weatherPricingService) {
        this.objectMapper = objectMapper;
        this.weatherPricingService = weatherPricingService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    private static final String SYSTEM_PROMPT_BEST_DAY =
            "Tu es un assistant de planification de livraisons agricoles. "
                    + "Tu reponds UNIQUEMENT a une seule question: quel est le meilleur jour pour effectuer une livraison, "
                    + "en te basant sur les previsions meteo et la majoration appliquee sur le prix de livraison. "
                    + "Reponds toujours en francais. "
                    + "Si l'utilisateur pose une question hors sujet (recettes, marche, engrais, sante, etc.), "
                    + "refuse poliment en disant que tu ne reponds qu'a la question du meilleur jour pour planifier une livraison. "
                    + "Utilise STRICTEMENT les donnees meteo fournies dans le message utilisateur: ne les invente pas, ne prends pas d'autres dates. "
                    + "Explique brievement pourquoi le jour choisi est le meilleur (meteo calme, majoration faible ou nulle) "
                    + "et mentionne le jour a eviter si pertinent. Reste concis (3 a 6 phrases).";

    public FarmerChatbotResponse askBestDeliveryDay(BestDayChatbotRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requete vide.");
        }
        String userMessage = request.message() == null ? "" : request.message().trim();
        if (userMessage.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message vide.");
        }
        if (request.pickupLat() == null || request.pickupLng() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Coordonnees de ramassage manquantes pour consulter la meteo.");
        }
        LocalDate fromDate = request.fromDate() != null ? request.fromDate() : LocalDate.now();
        LocalDate toDate = request.toDate() != null ? request.toDate() : fromDate.plusDays(6);
        if (toDate.isBefore(fromDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        List<WeatherPricingService.DailyForecast> forecast = weatherPricingService.fetchDailyForecast(
                request.pickupLat(), request.pickupLng(), fromDate, toDate);
        if (forecast.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Previsions meteo indisponibles pour la periode demandee.");
        }

        String forecastBlock = buildForecastBlock(forecast);
        String contextualMessage = "Donnees meteo jour par jour (a utiliser strictement):\n"
                + forecastBlock
                + "\n\nQuestion de l'agriculteur:\n\""
                + userMessage + "\"\n\n"
                + "Recommande le meilleur jour parmi ceux listes, explique pourquoi "
                + "et mentionne les jours a eviter s'il y a majoration.";

        return invokeGroq(SYSTEM_PROMPT_BEST_DAY, contextualMessage);
    }

    private String buildForecastBlock(List<WeatherPricingService.DailyForecast> forecast) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH);
        for (WeatherPricingService.DailyForecast day : forecast) {
            double surchargePct = Math.round(day.surchargePercent() * 1000.0) / 10.0;
            sb.append("- ")
                    .append(day.date().format(dateFormat))
                    .append(" (")
                    .append(day.date())
                    .append("): condition=").append(day.condition())
                    .append(", precipitation=").append(round1(day.precipitationMm())).append("mm")
                    .append(", vent_max=").append(round1(day.windSpeedKmh())).append("km/h");
            if (!Double.isNaN(day.temperatureMin()) && !Double.isNaN(day.temperatureMax())) {
                sb.append(", temp=").append(round1(day.temperatureMin())).append("/").append(round1(day.temperatureMax())).append("C");
            }
            sb.append(", majoration_livraison=+").append(surchargePct).append("%\n");
        }
        return sb.toString();
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public FarmerChatbotResponse askFarmerAssistant(String message) {
        String sanitized = message == null ? "" : message.trim();
        if (sanitized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message vide.");
        }
        return invokeGroq(SYSTEM_PROMPT_BEST_DAY, sanitized);
    }

    private FarmerChatbotResponse invokeGroq(String systemPrompt, String userContent) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Le chatbot n'est pas configure (GROQ_API_KEY manquant).");
        }

        Map<String, Object> payload = Map.of(
                "model", groqModel,
                "temperature", 0.2,
                "messages", new Object[] {
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", userContent
                        )
                }
        );

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(groqApiUrl))
                    .timeout(Duration.ofMillis(Math.max(groqTimeoutMs, 1000)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Le service chatbot est indisponible pour le moment.");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choice = root.path("choices").isArray() && !root.path("choices").isEmpty()
                    ? root.path("choices").get(0)
                    : null;
            String reply = choice != null ? choice.path("message").path("content").asText("").trim() : "";
            if (reply.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Reponse invalide du chatbot.");
            }

            String model = root.path("model").asText(groqModel);
            return new FarmerChatbotResponse(reply, model, LocalDateTime.now());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Erreur de communication avec le service chatbot.");
        }
    }
}
