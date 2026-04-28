package org.example.gestioninventaire.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.gestioninventaire.entities.VaccinationCampaign;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service optionnel backend : crée un événement Google Calendar
 * via un access token OAuth2 transmis depuis le frontend.
 *
 * Endpoint Angular → POST /api/vaccinations/campaign/{id}/calendar
 * Body : { "accessToken": "ya29...." }
 */
@Service
@Slf4j
public class GoogleCalendarBackendService {

    private static final String CALENDAR_API =
            "https://www.googleapis.com/calendar/v3/calendars/primary/events";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Crée un événement Google Calendar pour une campagne de vaccination.
     *
     * @param campaign    La campagne
     * @param productName Le nom du vaccin (pour la description)
     * @param accessToken Le token OAuth2 de l'utilisateur (venant du frontend)
     * @return L'URL de l'événement créé, ou null en cas d'erreur
     */
    public String createCalendarEvent(VaccinationCampaign campaign,
                                      String productName,
                                      String accessToken) {
        try {
            String dateStr = campaign.getPlannedDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);

            Map<String, Object> event = new HashMap<>();
            event.put("summary", "💉 Vaccination " + campaign.getEspece());
            event.put("description",
                    "Campagne de vaccination\n" +
                            "Espèce : " + campaign.getEspece() + "\n" +
                            "Tranche d'âge : " + campaign.getAgeMin() + " – " + campaign.getAgeMax() + " ans\n" +
                            "Vaccin : " + (productName != null ? productName : "N/A") + "\n" +
                            "Dose par animal : " + campaign.getDose() + " unités"
            );
            event.put("start", Map.of("date", dateStr));
            event.put("end", Map.of("date", dateStr));
            event.put("colorId", "2"); // vert
            event.put("reminders", Map.of(
                    "useDefault", false,
                    "overrides", List.of(
                            Map.of("method", "popup", "minutes", 1440), // 24h avant
                            Map.of("method", "popup", "minutes", 60)    // 1h avant
                    )
            ));

            String body = objectMapper.writeValueAsString(event);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CALENDAR_API))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                Map<?, ?> result = objectMapper.readValue(response.body(), Map.class);
                String htmlLink = (String) result.get("htmlLink");
                log.info("Événement Google Calendar créé : {}", htmlLink);
                return htmlLink;
            } else {
                log.error("Erreur Google Calendar API : {} - {}", response.statusCode(), response.body());
                return null;
            }

        } catch (IOException | InterruptedException e) {
            log.error("Exception lors de la création de l'événement Calendar", e);
            return null;
        }
    }
}