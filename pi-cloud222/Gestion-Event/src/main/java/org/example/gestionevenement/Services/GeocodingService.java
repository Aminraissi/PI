package org.example.gestionevenement.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeocodingService {

    private final RestTemplate rest = new RestTemplate();

    private final String API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjJiMzQ5MjA1YTYxNTRlMzdhZTdmOTNjNWU4YTAwZDJjIiwiaCI6Im11cm11cjY0In0=";

    public GeocodingService() {
        rest.getInterceptors().add((req, body, exec) -> {
            req.getHeaders().set("Authorization", API_KEY);
            req.getHeaders().set("Content-Type", "application/json");
            return exec.execute(req, body);
        });
    }

    public double[] geocode(String text) {

        String url = "https://api.openrouteservice.org/geocode/search?text="
                + text + ", Tunisia";

        String resp = rest.getForObject(url, String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(resp);

            JsonNode coords = root
                    .get("features")
                    .get(0)
                    .get("geometry")
                    .get("coordinates");

            return new double[]{
                    coords.get(1).asDouble(),
                    coords.get(0).asDouble()
            };

        } catch (Exception e) {
            return null;
        }
    }
}
